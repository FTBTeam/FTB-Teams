package com.feed_the_beast.mods.ftbteams.impl;

import com.feed_the_beast.mods.ftbteams.FTBTeams;
import com.feed_the_beast.mods.ftbteams.ProfileUtils;
import com.feed_the_beast.mods.ftbteams.api.Team;
import com.feed_the_beast.mods.ftbteams.api.TeamArgument;
import com.feed_the_beast.mods.ftbteams.api.TeamProperty;
import com.feed_the_beast.mods.ftbteams.api.property.BooleanProperty;
import com.feed_the_beast.mods.ftbteams.api.property.ColorProperty;
import com.feed_the_beast.mods.ftbteams.api.property.StringProperty;
import com.feed_the_beast.mods.ftbteams.event.PlayerJoinedTeamEvent;
import com.feed_the_beast.mods.ftbteams.event.PlayerLeftTeamEvent;
import com.feed_the_beast.mods.ftbteams.event.PlayerTransferredTeamOwnershipEvent;
import com.feed_the_beast.mods.ftbteams.event.TeamConfigEvent;
import com.feed_the_beast.mods.ftbteams.event.TeamCreatedEvent;
import com.feed_the_beast.mods.ftbteams.event.TeamDeletedEvent;
import com.feed_the_beast.mods.ftbteams.event.TeamLoadedEvent;
import com.feed_the_beast.mods.ftbteams.event.TeamSavedEvent;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class TeamImpl implements Team, INBTSerializable<CompoundNBT>
{
	public static final StringProperty DISPLAY_NAME = new StringProperty(new ResourceLocation(FTBTeams.MOD_ID, "display_name"), "");
	public static final StringProperty DESCRIPTION = new StringProperty(new ResourceLocation(FTBTeams.MOD_ID, "description"), "");
	public static final ColorProperty COLOR = new ColorProperty(new ResourceLocation(FTBTeams.MOD_ID, "color"), 0xFFFFFF);
	public static final BooleanProperty FREE_TO_JOIN = new BooleanProperty(new ResourceLocation(FTBTeams.MOD_ID, "free_to_join"), false);

	public final TeamManagerImpl manager;
	private boolean shouldSave;
	int id;
	GameProfile owner;
	final HashSet<GameProfile> members;
	final HashSet<GameProfile> invited;
	final HashSet<GameProfile> allies;
	private Map<TeamProperty, Object> properties;
	boolean serverTeam;

	public TeamImpl(TeamManagerImpl m)
	{
		id = 0;
		manager = m;
		owner = ProfileUtils.NO_PROFILE;
		members = new HashSet<>();
		invited = new HashSet<>();
		allies = new HashSet<>();
		properties = new HashMap<>();
		serverTeam = false;
	}

	private static int rgb(float r, float g, float b)
	{
		return 0xFF000000 | ((int) r << 16) | ((int) g << 8) | ((int) b);
	}

	public static int randomColor(Random rand)
	{
		float hue = rand.nextFloat();

		float h = (hue - (float) Math.floor(hue)) * 6F;
		float f = h - (float) Math.floor(h);
		float q = 1F - f;
		float t = 1F - (1F - f);

		switch ((int) h)
		{
			case 0:
				return rgb(255F + 0.5F, t * 255F + 0.5F, 0.5F);
			case 1:
				return rgb(q * 255F + 0.5F, 255F + 0.5F, 0.5F);
			case 2:
				return rgb(0.5F, 255F + 0.5F, t * 255F + 0.5F);
			case 3:
				return rgb(0.5F, q * 255F + 0.5F, 255F + 0.5F);
			case 4:
				return rgb(t * 255F + 0.5F, 0.5F, 255F + 0.5F);
			default:
				return rgb(255F + 0.5F, 0.5F, q * 255F + 0.5F);
		}
	}

	@Override
	public int hashCode()
	{
		return id;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
		{
			return true;
		}
		else if (o instanceof Team)
		{
			return id == ((Team) o).getId();
		}

		return false;
	}

	@Override
	public String toString()
	{
		return owner == ProfileUtils.NO_PROFILE ? String.valueOf(getId()) : owner.getName();
	}

	@Override
	public TeamManagerImpl getManager()
	{
		return manager;
	}

	@Override
	public void save()
	{
		shouldSave = true;
		manager.nameMap = null;
	}

	public void saveFile(File directory) throws IOException
	{
		if (shouldSave)
		{
			File file = new File(directory, getId() + ".nbt");

			if (!file.getParentFile().exists())
			{
				file.getParentFile().mkdirs();
			}

			CompressedStreamTools.write(serializeNBT(), file);
			shouldSave = false;
		}
	}

	@Override
	public int getId()
	{
		return id;
	}

	@Override
	public String getStringID()
	{
		String s = getProperty(DISPLAY_NAME).replaceAll("\\W", "");
		return (s.length() > 50 ? s.substring(0, 50) : s) + "#" + getId();
	}

	@Override
	public ITextComponent getName()
	{
		ITextComponent text = new StringTextComponent(getProperty(DISPLAY_NAME));
		text.getStyle().setColor(TextFormatting.AQUA);
		text.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams info " + getStringID()));
		return text;
	}

	@Override
	public boolean isServerTeam()
	{
		return serverTeam;
	}

	@Override
	public <T> T getProperty(TeamProperty<T> property)
	{
		Object o = properties.get(property);
		return o == null ? property.defaultValue : (T) o;
	}

	@Override
	public <T> void setProperty(TeamProperty<T> property, T value)
	{
		properties.put(property, value);
		save();
	}

	@Override
	public boolean create()
	{
		manager.lastUID++;
		id = manager.lastUID;
		manager.teamMap.put(id, this);
		MinecraftForge.EVENT_BUS.post(new TeamCreatedEvent(this));
		save();
		manager.save();
		return true;
	}

	@Override
	public boolean delete()
	{
		if (!manager.teamMap.containsKey(id))
		{
			return false;
		}

		File directory = new File(manager.getServer().getWorld(DimensionType.OVERWORLD).getSaveHandler().getWorldDirectory(), "data/ftbteams");
		File deletedDirectory = new File(directory, "deleted");

		try
		{
			if (!deletedDirectory.exists())
			{
				deletedDirectory.mkdirs();
			}

			CompressedStreamTools.write(serializeNBT(), new File(deletedDirectory, getId() + ".nbt"));
		}
		catch (Exception ex)
		{
		}

		Set<GameProfile> prevMembers = Collections.unmodifiableSet(new HashSet<>(members));

		for (GameProfile profile : prevMembers)
		{
			removeMember(profile, false);
		}

		MinecraftForge.EVENT_BUS.post(new TeamDeletedEvent(this, prevMembers));
		manager.teamMap.remove(id);
		new File(directory, getId() + ".nbt").delete();
		return true;
	}

	@Override
	public boolean isOwner(GameProfile profile)
	{
		return owner.equals(profile);
	}

	@Override
	public GameProfile getOwner()
	{
		return owner;
	}

	@Override
	@Nullable
	public ServerPlayerEntity getOwnerPlayer()
	{
		return ProfileUtils.getPlayerByProfile(manager.server, owner);
	}

	@Override
	public boolean isMember(GameProfile profile)
	{
		return members.contains(profile);
	}

	@Override
	public Set<GameProfile> getMembers()
	{
		return Collections.unmodifiableSet(members);
	}

	@Override
	public List<ServerPlayerEntity> getOnlineMembers()
	{
		List<ServerPlayerEntity> list = new ArrayList<>();

		for (GameProfile member : members)
		{
			ServerPlayerEntity player = ProfileUtils.getPlayerByProfile(manager.server, member);

			if (player != null)
			{
				list.add(player);
			}
		}

		return list;
	}

	@Override
	public boolean addMember(ServerPlayerEntity player)
	{
		Optional<Team> oldTeam = manager.getTeam(player);
		Team ot = oldTeam.orElse(null);

		if (ot == this)
		{
			return false;
		}
		else if (ot != null)
		{
			ot.removeMember(player.getGameProfile(), false);
		}

		manager.playerTeamMap.put(player.getGameProfile(), this);
		members.add(player.getGameProfile());
		save();
		MinecraftForge.EVENT_BUS.post(new PlayerJoinedTeamEvent(this, oldTeam, player));

		if (ot != null && ot.getMembers().isEmpty())
		{
			ot.delete();
		}

		return true;
	}

	@Override
	public boolean removeMember(GameProfile profile, boolean deleteWhenEmpty)
	{
		if (!members.remove(profile))
		{
			return false;
		}

		manager.playerTeamMap.remove(profile);
		MinecraftForge.EVENT_BUS.post(new PlayerLeftTeamEvent(this, profile));

		if (deleteWhenEmpty && members.isEmpty())
		{
			delete();
		}

		save();
		return true;
	}

	@Override
	public boolean isAlly(GameProfile profile)
	{
		return isOwner(profile) || isMember(profile);
	}

	@Override
	public Set<GameProfile> getAllies()
	{
		return allies;
	}

	@Override
	public boolean isInvited(GameProfile profile)
	{
		return getProperty(FREE_TO_JOIN) || !isOwner(profile) && !isMember(profile) && invited.contains(profile);
	}

	@Override
	public Set<GameProfile> getInvited()
	{
		return invited;
	}

	// Data IO //

	@Override
	public CompoundNBT serializeNBT()
	{
		CompoundNBT nbt = new CompoundNBT();
		nbt.putInt("id", id);
		nbt.putBoolean("server_team", serverTeam);
		nbt.putString("owner", ProfileUtils.serializeProfile(owner));

		ListNBT membersNBT = new ListNBT();

		for (GameProfile member : members)
		{
			membersNBT.add(StringNBT.valueOf(ProfileUtils.serializeProfile(member)));
		}

		nbt.put("members", membersNBT);

		ListNBT invitedNBT = new ListNBT();

		for (GameProfile invited : invited)
		{
			invitedNBT.add(StringNBT.valueOf(ProfileUtils.serializeProfile(invited)));
		}

		nbt.put("invited", invitedNBT);

		ListNBT alliesNBT = new ListNBT();

		for (GameProfile ally : allies)
		{
			alliesNBT.add(StringNBT.valueOf(ProfileUtils.serializeProfile(ally)));
		}

		nbt.put("allies", alliesNBT);

		CompoundNBT propertiesNBT = new CompoundNBT();

		for (Map.Entry<TeamProperty, Object> property : properties.entrySet())
		{
			propertiesNBT.putString(property.getKey().id.toString(), property.getKey().toString(property.getValue()));
		}

		nbt.put("properties", propertiesNBT);

		CompoundNBT extra = new CompoundNBT();
		MinecraftForge.EVENT_BUS.post(new TeamSavedEvent(this, extra));
		nbt.put("extra", extra);

		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundNBT nbt)
	{
		serverTeam = nbt.getBoolean("server_team");
		owner = ProfileUtils.deserializeProfile(nbt.getString("owner"));

		members.clear();
		ListNBT membersNBT = nbt.getList("members", Constants.NBT.TAG_STRING);

		for (int i = 0; i < membersNBT.size(); i++)
		{
			GameProfile profile = ProfileUtils.deserializeProfile(membersNBT.getString(i));

			if (profile != ProfileUtils.NO_PROFILE)
			{
				members.add(profile);
			}
		}

		invited.clear();
		ListNBT invitedNBT = nbt.getList("invited", Constants.NBT.TAG_STRING);

		for (int i = 0; i < invitedNBT.size(); i++)
		{
			GameProfile profile = ProfileUtils.deserializeProfile(invitedNBT.getString(i));

			if (profile != ProfileUtils.NO_PROFILE)
			{
				invited.add(profile);
			}
		}

		allies.clear();
		ListNBT alliesNBT = nbt.getList("allies", Constants.NBT.TAG_STRING);

		for (int i = 0; i < alliesNBT.size(); i++)
		{
			GameProfile profile = ProfileUtils.deserializeProfile(alliesNBT.getString(i));

			if (profile != ProfileUtils.NO_PROFILE)
			{
				allies.add(profile);
			}
		}

		properties.clear();
		CompoundNBT propertiesNBT = nbt.getCompound("properties");

		Map<String, TeamProperty> map = new HashMap<>();
		MinecraftForge.EVENT_BUS.post(new TeamConfigEvent(p -> map.put(p.id.toString(), p)));

		for (String key : propertiesNBT.keySet())
		{
			TeamProperty property = map.get(key);

			if (property != null && property.isValidFor(this))
			{
				Optional optional = property.fromString(propertiesNBT.getString(key));

				if (optional.isPresent())
				{
					properties.put(property, optional.get());
				}
			}
		}

		MinecraftForge.EVENT_BUS.post(new TeamLoadedEvent(this, nbt.getCompound("extra")));
	}

	// Commands //

	int delete(CommandSource source) throws CommandSyntaxException
	{
		if (delete())
		{
			source.sendFeedback(new StringTextComponent("Deleted " + getId()), true);
		}

		return Command.SINGLE_SUCCESS;
	}

	int settings(CommandSource source, TeamProperty key, String value) throws CommandSyntaxException
	{
		if (value.isEmpty())
		{
			ITextComponent keyc = new StringTextComponent(key.id.toString());
			keyc.getStyle().setColor(TextFormatting.YELLOW);
			ITextComponent valuec = new StringTextComponent(key.toString(getProperty(key)));
			valuec.getStyle().setColor(TextFormatting.AQUA);
			source.sendFeedback(new StringTextComponent("").appendSibling(keyc).appendText(" is set to ").appendSibling(valuec), true);
		}
		else
		{
			Optional optional = key.fromString(value);

			if (!optional.isPresent())
			{
				//throw CommandSyntaxException
				source.sendFeedback(new StringTextComponent("Failed to parse value!"), true);
				return 0;
			}

			setProperty(key, optional.get());
			ITextComponent keyc = new StringTextComponent(key.id.toString());
			keyc.getStyle().setColor(TextFormatting.YELLOW);
			ITextComponent valuec = new StringTextComponent(value);
			valuec.getStyle().setColor(TextFormatting.AQUA);
			source.sendFeedback(new StringTextComponent("Set ").appendSibling(keyc).appendText(" to ").appendSibling(valuec), true);
		}

		return Command.SINGLE_SUCCESS;
	}

	int join(CommandSource source) throws CommandSyntaxException
	{
		ServerPlayerEntity player = source.asPlayer();
		Optional<Team> oldTeam = manager.getTeam(player);

		if (oldTeam.isPresent())
		{
			throw TeamArgument.ALREADY_IN_TEAM.create();
		}

		if (!isInvited(player))
		{
			throw TeamArgument.NOT_INVITED.create(getName());
		}

		invited.remove(player.getGameProfile());
		addMember(player);

		for (ServerPlayerEntity member : getOnlineMembers())
		{
			member.sendMessage(new StringTextComponent("").appendSibling(player.getDisplayName()).appendText(" joined ").appendSibling(getName()));
		}

		return Command.SINGLE_SUCCESS;
	}

	int leave(CommandSource source) throws CommandSyntaxException
	{
		ServerPlayerEntity player = source.asPlayer();

		List<ServerPlayerEntity> onlineMembers = getOnlineMembers();

		if (removeMember(player.getGameProfile(), true))
		{
			for (ServerPlayerEntity member : onlineMembers)
			{
				member.sendMessage(new StringTextComponent("").appendSibling(player.getDisplayName()).appendText(" left ").appendSibling(getName()));
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	int invite(CommandSource source, Collection<GameProfile> players) throws CommandSyntaxException
	{
		if (!getProperty(FREE_TO_JOIN) && invited.addAll(players))
		{
			save();
		}

		for (GameProfile player : players)
		{
			ServerPlayerEntity playerEntity = ProfileUtils.getPlayerByProfile(manager.getServer(), player);

			if (playerEntity != null)
			{
				source.sendFeedback(new StringTextComponent("Invited ").appendSibling(playerEntity.getDisplayName()).appendText(" to ").appendSibling(getName()), true);
				ITextComponent component = new StringTextComponent("You have been invited to ").appendSibling(getName()).appendText("!");
				playerEntity.sendMessage(component);

				ITextComponent accept = new StringTextComponent("[Click here to accept the invite]");
				accept.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ftbteams join " + getStringID()));
				accept.getStyle().setColor(TextFormatting.GREEN);
				playerEntity.sendMessage(accept);

				ITextComponent deny = new StringTextComponent("[Click here to deny the invite]");
				deny.getStyle().setColor(TextFormatting.RED);
				deny.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ftbteams deny_invite " + getStringID()));
				playerEntity.sendMessage(deny);
			}
			else
			{
				source.sendFeedback(new StringTextComponent("Invited " + player.getName() + " to ").appendSibling(getName()), true);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	int denyInvite(CommandSource source) throws CommandSyntaxException
	{
		if (invited.remove(source.asPlayer().getGameProfile()))
		{
			source.sendFeedback(new StringTextComponent("Invite denied"), true);
			save();
		}

		return Command.SINGLE_SUCCESS;
	}

	int kick(CommandSource source, Collection<GameProfile> players) throws CommandSyntaxException
	{
		invited.removeAll(players);

		for (GameProfile player : players)
		{
			ServerPlayerEntity playerEntity = ProfileUtils.getPlayerByProfile(manager.getServer(), player);

			if (playerEntity != null)
			{
				source.sendFeedback(new StringTextComponent("Kicked ").appendSibling(playerEntity.getDisplayName()).appendText(" from ").appendSibling(getName()), true);
				playerEntity.sendMessage(new StringTextComponent("You have been kicked from ").appendSibling(getName()).appendText("!"));
			}
			else
			{
				source.sendFeedback(new StringTextComponent("Kicked " + player.getName() + " from ").appendSibling(getName()), true);
			}

			removeMember(player, true);
		}

		return Command.SINGLE_SUCCESS;
	}

	int transferOwnership(CommandSource source, ServerPlayerEntity to) throws CommandSyntaxException
	{
		ServerPlayerEntity from = source.asPlayer();

		if (!isMember(to))
		{
			throw TeamArgument.NOT_MEMBER.create(to.getDisplayName(), getName());
		}

		owner = to.getGameProfile();
		save();
		MinecraftForge.EVENT_BUS.post(new PlayerTransferredTeamOwnershipEvent(this, from, to));
		source.sendFeedback(new StringTextComponent("Transferred ownership to ").appendSibling(to.getDisplayName()), true);
		return Command.SINGLE_SUCCESS;
	}

	int info(CommandSource source) throws CommandSyntaxException
	{
		ITextComponent infoComponent = new StringTextComponent("");
		infoComponent.getStyle().setBold(true);
		infoComponent.appendText("== ");
		infoComponent.appendSibling(getName());
		infoComponent.appendText(" ==");
		source.sendFeedback(infoComponent, true);

		ITextComponent idComponent = new StringTextComponent(String.valueOf(id));
		idComponent.getStyle().setColor(TextFormatting.YELLOW);
		source.sendFeedback(new TranslationTextComponent("ftbteams.info.id", idComponent), true);

		ITextComponent ownerComponent = new StringTextComponent(getOwner().getName());
		ownerComponent.getStyle().setColor(TextFormatting.YELLOW);
		source.sendFeedback(new TranslationTextComponent("ftbteams.info.owner", ownerComponent), true);

		source.sendFeedback(new TranslationTextComponent("ftbteams.info.members"), true);

		for (GameProfile member : getMembers())
		{
			ITextComponent memberComponent = new StringTextComponent("- " + member.getName());
			memberComponent.getStyle().setColor(TextFormatting.YELLOW);
			source.sendFeedback(memberComponent, true);
		}

		return Command.SINGLE_SUCCESS;
	}

	int msg(CommandSource source, String message) throws CommandSyntaxException
	{
		ITextComponent component = new StringTextComponent("<");
		component.appendSibling(source.getDisplayName().deepCopy().applyTextStyle(TextFormatting.YELLOW));
		component.appendText(" @");
		component.appendSibling(getName());
		component.appendText("> ");
		component.appendSibling(ForgeHooks.newChatWithLinks(message));

		for (ServerPlayerEntity player : getOnlineMembers())
		{
			player.sendMessage(component);
		}

		return Command.SINGLE_SUCCESS;
	}

	int gui(CommandSource source) throws CommandSyntaxException
	{
		if (ModList.get().isLoaded("ftbguilibrary"))
		{
			openGUI(source.asPlayer());
			return Command.SINGLE_SUCCESS;
		}

		throw TeamArgument.NO_GUI_LIBRARY.create();
	}

	public void openGUI(ServerPlayerEntity player)
	{
		if (ModList.get().isLoaded("ftbguilibrary"))
		{
			//TODO: Add gui when FTB GUI Library is done
		}
	}
}