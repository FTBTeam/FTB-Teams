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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
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
public class TeamImpl implements Team, INBTSerializable<CompoundTag>
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
	private final Map<TeamProperty, Object> properties;
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

			NbtIo.write(serializeNBT(), file);
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
	public Component getName()
	{
		TextComponent text = new TextComponent(getProperty(DISPLAY_NAME));
		text.withStyle(ChatFormatting.AQUA);
		text.setStyle(text.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams info " + getStringID())));
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

		File directory = manager.getServer().getWorldPath(new LevelResource("data/ftbteams")).toFile();
		File deletedDirectory = new File(directory, "deleted");

		try
		{
			if (!deletedDirectory.exists())
			{
				deletedDirectory.mkdirs();
			}

			NbtIo.write(serializeNBT(), new File(deletedDirectory, getId() + ".nbt"));
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
	public ServerPlayer getOwnerPlayer()
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
	public List<ServerPlayer> getOnlineMembers()
	{
		List<ServerPlayer> list = new ArrayList<>();

		for (GameProfile member : members)
		{
			ServerPlayer player = ProfileUtils.getPlayerByProfile(manager.server, member);

			if (player != null)
			{
				list.add(player);
			}
		}

		return list;
	}

	@Override
	public boolean addMember(ServerPlayer player)
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
	public CompoundTag serializeNBT()
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("id", id);
		nbt.putBoolean("server_team", serverTeam);
		nbt.putString("owner", ProfileUtils.serializeProfile(owner));

		ListTag membersNBT = new ListTag();

		for (GameProfile member : members)
		{
			membersNBT.add(StringTag.valueOf(ProfileUtils.serializeProfile(member)));
		}

		nbt.put("members", membersNBT);

		ListTag invitedNBT = new ListTag();

		for (GameProfile invited : invited)
		{
			invitedNBT.add(StringTag.valueOf(ProfileUtils.serializeProfile(invited)));
		}

		nbt.put("invited", invitedNBT);

		ListTag alliesNBT = new ListTag();

		for (GameProfile ally : allies)
		{
			alliesNBT.add(StringTag.valueOf(ProfileUtils.serializeProfile(ally)));
		}

		nbt.put("allies", alliesNBT);

		CompoundTag propertiesNBT = new CompoundTag();

		for (Map.Entry<TeamProperty, Object> property : properties.entrySet())
		{
			propertiesNBT.putString(property.getKey().id.toString(), property.getKey().toString(property.getValue()));
		}

		nbt.put("properties", propertiesNBT);

		CompoundTag extra = new CompoundTag();
		MinecraftForge.EVENT_BUS.post(new TeamSavedEvent(this, extra));
		nbt.put("extra", extra);

		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt)
	{
		serverTeam = nbt.getBoolean("server_team");
		owner = ProfileUtils.deserializeProfile(nbt.getString("owner"));

		members.clear();
		ListTag membersNBT = nbt.getList("members", Constants.NBT.TAG_STRING);

		for (int i = 0; i < membersNBT.size(); i++)
		{
			GameProfile profile = ProfileUtils.deserializeProfile(membersNBT.getString(i));

			if (profile != ProfileUtils.NO_PROFILE)
			{
				members.add(profile);
			}
		}

		invited.clear();
		ListTag invitedNBT = nbt.getList("invited", Constants.NBT.TAG_STRING);

		for (int i = 0; i < invitedNBT.size(); i++)
		{
			GameProfile profile = ProfileUtils.deserializeProfile(invitedNBT.getString(i));

			if (profile != ProfileUtils.NO_PROFILE)
			{
				invited.add(profile);
			}
		}

		allies.clear();
		ListTag alliesNBT = nbt.getList("allies", Constants.NBT.TAG_STRING);

		for (int i = 0; i < alliesNBT.size(); i++)
		{
			GameProfile profile = ProfileUtils.deserializeProfile(alliesNBT.getString(i));

			if (profile != ProfileUtils.NO_PROFILE)
			{
				allies.add(profile);
			}
		}

		properties.clear();
		CompoundTag propertiesNBT = nbt.getCompound("properties");

		Map<String, TeamProperty> map = new HashMap<>();
		MinecraftForge.EVENT_BUS.post(new TeamConfigEvent(p -> map.put(p.id.toString(), p)));

		for (String key : propertiesNBT.getAllKeys())
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

	int delete(CommandSourceStack source) throws CommandSyntaxException
	{
		if (delete())
		{
			source.sendSuccess(new TextComponent("Deleted " + getId()), true);
		}

		return Command.SINGLE_SUCCESS;
	}

	int settings(CommandSourceStack source, TeamProperty key, String value) throws CommandSyntaxException
	{
		if (value.isEmpty())
		{
			TextComponent keyc = new TextComponent(key.id.toString());
			keyc.withStyle(ChatFormatting.YELLOW);
			TextComponent valuec = new TextComponent(key.toString(getProperty(key)));
			valuec.withStyle(ChatFormatting.AQUA);
			source.sendSuccess(new TextComponent("").append(keyc).append(" is set to ").append(valuec), true);
		}
		else
		{
			Optional optional = key.fromString(value);

			if (!optional.isPresent())
			{
				//throw CommandSyntaxException
				source.sendSuccess(new TextComponent("Failed to parse value!"), true);
				return 0;
			}

			setProperty(key, optional.get());
			TextComponent keyc = new TextComponent(key.id.toString());
			keyc.withStyle(ChatFormatting.YELLOW);
			TextComponent valuec = new TextComponent(value);
			valuec.withStyle(ChatFormatting.AQUA);
			source.sendSuccess(new TextComponent("Set ").append(keyc).append(" to ").append(valuec), true);
		}

		return Command.SINGLE_SUCCESS;
	}

	int join(CommandSourceStack source) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();
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

		for (ServerPlayer member : getOnlineMembers())
		{
			member.displayClientMessage(new TextComponent("").append(player.getDisplayName()).append(" joined ").append(getName()), false);
		}

		return Command.SINGLE_SUCCESS;
	}

	int leave(CommandSourceStack source) throws CommandSyntaxException
	{
		ServerPlayer player = source.getPlayerOrException();

		List<ServerPlayer> onlineMembers = getOnlineMembers();

		if (removeMember(player.getGameProfile(), true))
		{
			for (ServerPlayer member : onlineMembers)
			{
				member.displayClientMessage(new TextComponent("").append(player.getDisplayName()).append(" left ").append(getName()), false);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	int invite(CommandSourceStack source, Collection<GameProfile> players) throws CommandSyntaxException
	{
		if (!getProperty(FREE_TO_JOIN) && invited.addAll(players))
		{
			save();
		}

		for (GameProfile player : players)
		{
			ServerPlayer playerEntity = ProfileUtils.getPlayerByProfile(manager.getServer(), player);

			if (playerEntity != null)
			{
				source.sendSuccess(new TextComponent("Invited ").append(playerEntity.getDisplayName()).append(" to ").append(getName()), true);
				MutableComponent component = new TextComponent("You have been invited to ").append(getName()).append("!");
				playerEntity.displayClientMessage(component, false);

				TextComponent accept = new TextComponent("[Click here to accept the invite]");
				accept.setStyle(accept.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ftbteams join " + getStringID())));
				accept.withStyle(ChatFormatting.GREEN);
				playerEntity.displayClientMessage(accept, false);

				TextComponent deny = new TextComponent("[Click here to deny the invite]");
				deny.withStyle(ChatFormatting.RED);
				deny.setStyle(deny.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ftbteams deny_invite " + getStringID())));
				playerEntity.displayClientMessage(deny, false);
			}
			else
			{
				source.sendSuccess(new TextComponent("Invited " + player.getName() + " to ").append(getName()), true);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	int denyInvite(CommandSourceStack source) throws CommandSyntaxException
	{
		if (invited.remove(source.getPlayerOrException().getGameProfile()))
		{
			source.sendSuccess(new TextComponent("Invite denied"), true);
			save();
		}

		return Command.SINGLE_SUCCESS;
	}

	int kick(CommandSourceStack source, Collection<GameProfile> players) throws CommandSyntaxException
	{
		invited.removeAll(players);

		for (GameProfile player : players)
		{
			ServerPlayer playerEntity = ProfileUtils.getPlayerByProfile(manager.getServer(), player);

			if (playerEntity != null)
			{
				source.sendSuccess(new TextComponent("Kicked ").append(playerEntity.getDisplayName()).append(" from ").append(getName()), true);
				playerEntity.displayClientMessage(new TextComponent("You have been kicked from ").append(getName()).append("!"), false);
			}
			else
			{
				source.sendSuccess(new TextComponent("Kicked " + player.getName() + " from ").append(getName()), true);
			}

			removeMember(player, true);
		}

		return Command.SINGLE_SUCCESS;
	}

	int transferOwnership(CommandSourceStack source, ServerPlayer to) throws CommandSyntaxException
	{
		ServerPlayer from = source.getPlayerOrException();

		if (!isMember(to))
		{
			throw TeamArgument.NOT_MEMBER.create(to.getDisplayName(), getName());
		}

		owner = to.getGameProfile();
		save();
		MinecraftForge.EVENT_BUS.post(new PlayerTransferredTeamOwnershipEvent(this, from, to));
		source.sendSuccess(new TextComponent("Transferred ownership to ").append(to.getDisplayName()), true);
		return Command.SINGLE_SUCCESS;
	}

	int info(CommandSourceStack source) throws CommandSyntaxException
	{
		TextComponent infoComponent = new TextComponent("");
		infoComponent.getStyle().withBold(true);
		infoComponent.append("== ");
		infoComponent.append(getName());
		infoComponent.append(" ==");
		source.sendSuccess(infoComponent, true);

		TextComponent idComponent = new TextComponent(String.valueOf(id));
		idComponent.withStyle(ChatFormatting.YELLOW);
		source.sendSuccess(new TranslatableComponent("ftbteams.info.id", idComponent), true);

		TextComponent ownerComponent = new TextComponent(getOwner().getName());
		ownerComponent.withStyle(ChatFormatting.YELLOW);
		source.sendSuccess(new TranslatableComponent("ftbteams.info.owner", ownerComponent), true);

		source.sendSuccess(new TranslatableComponent("ftbteams.info.members"), true);

		for (GameProfile member : getMembers())
		{
			TextComponent memberComponent = new TextComponent("- " + member.getName());
			memberComponent.withStyle(ChatFormatting.YELLOW);
			source.sendSuccess(memberComponent, true);
		}

		return Command.SINGLE_SUCCESS;
	}

	int msg(CommandSourceStack source, String message) throws CommandSyntaxException
	{
		TextComponent component = new TextComponent("<");
		component.append(source.getDisplayName().copy().withStyle(ChatFormatting.YELLOW));
		component.append(" @");
		component.append(getName());
		component.append("> ");
		component.append(ForgeHooks.newChatWithLinks(message));

		for (ServerPlayer player : getOnlineMembers())
		{
			player.displayClientMessage(component, false);
		}

		return Command.SINGLE_SUCCESS;
	}

	int gui(CommandSourceStack source) throws CommandSyntaxException
	{
		if (ModList.get().isLoaded("ftbguilibrary"))
		{
			openGUI(source.getPlayerOrException());
			return Command.SINGLE_SUCCESS;
		}

		throw TeamArgument.NO_GUI_LIBRARY.create();
	}

	public void openGUI(ServerPlayer player)
	{
		if (ModList.get().isLoaded("ftbguilibrary"))
		{
			//TODO: Add gui when FTB GUI Library is done
		}
	}
}