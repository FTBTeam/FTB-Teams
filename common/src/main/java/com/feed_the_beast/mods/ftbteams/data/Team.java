package com.feed_the_beast.mods.ftbteams.data;

import com.feed_the_beast.mods.ftbteams.FTBTeams;
import com.feed_the_beast.mods.ftbteams.event.PlayerChangedTeamEvent;
import com.feed_the_beast.mods.ftbteams.event.TeamConfigEvent;
import com.feed_the_beast.mods.ftbteams.event.TeamDeletedEvent;
import com.feed_the_beast.mods.ftbteams.event.TeamLoadedEvent;
import com.feed_the_beast.mods.ftbteams.event.TeamSavedEvent;
import com.feed_the_beast.mods.ftbteams.property.BooleanProperty;
import com.feed_the_beast.mods.ftbteams.property.ColorProperty;
import com.feed_the_beast.mods.ftbteams.property.StringProperty;
import com.mojang.util.UUIDTypeAdapter;
import me.shedaniel.architectury.utils.NbtType;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public abstract class Team {
	public static final StringProperty DISPLAY_NAME = new StringProperty(new ResourceLocation(FTBTeams.MOD_ID, "display_name"), "Unknown");
	public static final StringProperty DESCRIPTION = new StringProperty(new ResourceLocation(FTBTeams.MOD_ID, "description"), "");
	public static final ColorProperty COLOR = new ColorProperty(new ResourceLocation(FTBTeams.MOD_ID, "color"), 0xFFFFFF);
	public static final BooleanProperty FREE_TO_JOIN = new BooleanProperty(new ResourceLocation(FTBTeams.MOD_ID, "free_to_join"), false);

	public final TeamManager manager;
	boolean shouldSave;
	UUID id;
	UUID owner;
	protected final HashSet<UUID> members;
	protected final Map<TeamProperty, Object> properties;
	private CompoundTag extraData;

	public Team(TeamManager m) {
		id = Util.NIL_UUID;
		manager = m;
		owner = Util.NIL_UUID;
		members = new HashSet<>();
		properties = new HashMap<>();
		extraData = new CompoundTag();
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (o instanceof Team) {
			return id.equals(((Team) o).getId());
		}

		return false;
	}

	@Override
	public String toString() {
		return getStringID();
	}

	public abstract TeamType getType();

	public void save() {
		shouldSave = true;
		manager.nameMap = null;
	}

	public UUID getId() {
		return id;
	}

	public CompoundTag getExtraData() {
		return extraData;
	}

	public String getDisplayName() {
		return getProperty(DISPLAY_NAME);
	}

	public int getColor() {
		return getProperty(COLOR) & 0xFFFFFF;
	}

	public String getStringID() {
		String s = getDisplayName().replaceAll("\\W", "");
		return (s.length() > 50 ? s.substring(0, 50) : s) + "#" + getId();
	}

	public Component getName() {
		TextComponent text = new TextComponent(getDisplayName());
		text.withStyle(ChatFormatting.AQUA);
		text.setStyle(text.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams info " + getStringID())));
		return text;
	}

	public <T> T getProperty(TeamProperty<T> property) {
		Object o = properties.get(property);
		return o == null ? property.defaultValue : (T) o;
	}

	public <T> void setProperty(TeamProperty<T> property, T value) {
		properties.put(property, value);
		save();
	}

	public boolean delete() {
		if (!manager.teamMap.containsKey(id)) {
			return false;
		}

		Path directory = manager.getServer().getWorldPath(TeamManager.FOLDER_NAME);
		Path deletedDirectory = directory.resolve("deleted");

		try {
			if (Files.notExists(deletedDirectory)) {
				Files.createDirectories(deletedDirectory);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try (OutputStream stream = Files.newOutputStream(deletedDirectory.resolve(getId() + ".nbt"))) {
			NbtIo.writeCompressed(serializeNBT(), stream);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Set<UUID> prevMembers = Collections.unmodifiableSet(new HashSet<>(members));

		for (UUID id : prevMembers) {
			removeMember(id, false);
		}

		TeamDeletedEvent.EVENT.invoker().accept(new TeamDeletedEvent(this, prevMembers));
		manager.teamMap.remove(id);
		directory.resolve(getId() + ".nbt").toFile().delete();
		return true;
	}

	public TeamRank getHighestRank(UUID playerId) {
		if (owner.equals(playerId)) {
			return TeamRank.OWNER;
		}

		if (getOfficers().contains(playerId)) {
			return TeamRank.OFFICER;
		}

		if (members.contains(playerId)) {
			return TeamRank.MEMBER;
		}

		// TODO: Implement enemies here

		if (getProperty(FREE_TO_JOIN) || getAllies().contains(playerId) || getInvited().contains(playerId)) {
			return TeamRank.ALLY;
		}

		return TeamRank.NONE;
	}

	public boolean isOwner(UUID profile) {
		return owner.equals(profile);
	}

	public boolean isOwner(ServerPlayer player) {
		return isOwner(player.getUUID());
	}

	public UUID getOwner() {
		return owner;
	}

	@Nullable
	public ServerPlayer getOwnerPlayer() {
		return FTBTUtils.getPlayerByUUID(manager.server, owner);
	}

	public boolean isMember(UUID uuid) {
		return members.contains(uuid);
	}

	public boolean isMember(ServerPlayer player) {
		return isMember(player.getUUID());
	}

	public Set<UUID> getMembers() {
		return members;
	}

	public List<ServerPlayer> getOnlineMembers() {
		List<ServerPlayer> list = new ArrayList<>();

		for (UUID member : members) {
			ServerPlayer player = FTBTUtils.getPlayerByUUID(manager.server, member);

			if (player != null) {
				list.add(player);
			}
		}

		return list;
	}

	public void changedTeam(Optional<Team> prev, UUID player) {
		PlayerChangedTeamEvent.EVENT.invoker().accept(new PlayerChangedTeamEvent(this, prev, player));
	}

	public boolean addMember(UUID uuid) {
		Team ot = manager.getPlayerTeam(uuid);

		if (ot == this) {
			return false;
		} else if (ot != null) {
			ot.removeMember(uuid, false);
		}

		manager.playerTeamMap.put(uuid, this);
		members.add(uuid);
		save();

		if (ot != null && ot.getMembers().isEmpty() && ot.getType().isParty()) {
			ot.delete();
		}

		return true;
	}

	public boolean removeMember(UUID id, boolean deleteWhenEmpty) {
		if (!members.remove(id)) {
			return false;
		}

		if (!getType().isPlayer()) {
			manager.playerTeamMap.put(id, manager.getPlayerTeam(id));
		}

		if (deleteWhenEmpty && members.isEmpty() && getType().isParty()) {
			delete();
		} else {
			save();
		}

		return true;
	}

	public boolean isAlly(UUID profile) {
		return isOwner(profile) || isMember(profile) || getAllies().contains(profile) || isInvited(profile);
	}

	public boolean isAlly(ServerPlayer player) {
		return isAlly(player.getUUID());
	}

	public Set<UUID> getAllies() {
		return Collections.emptySet();
	}

	public boolean isOfficer(UUID profile) {
		return isOwner(profile) || getOfficers().contains(profile);
	}

	public boolean isOfficer(ServerPlayer player) {
		return isOfficer(player.getUUID());
	}

	public Set<UUID> getOfficers() {
		return Collections.emptySet();
	}

	public boolean isInvited(UUID profile) {
		return getProperty(FREE_TO_JOIN) || !isOwner(profile) && !isMember(profile) && getInvited().contains(profile);
	}

	public boolean isInvited(ServerPlayer player) {
		return isInvited(player.getUUID());
	}

	public Set<UUID> getInvited() {
		return Collections.emptySet();
	}

	// Data IO //

	public CompoundTag serializeNBT() {
		CompoundTag tag = new OrderedCompoundTag();
		tag.putString("id", UUIDTypeAdapter.fromUUID(getId()));
		tag.putString("type", getType().getSerializedName());
		tag.putString("owner", UUIDTypeAdapter.fromUUID(getOwner()));

		ListTag membersNBT = new ListTag();

		for (UUID member : members) {
			membersNBT.add(StringTag.valueOf(UUIDTypeAdapter.fromUUID(member)));
		}

		tag.put("members", membersNBT);

		CompoundTag propertiesNBT = new CompoundTag();

		for (Map.Entry<TeamProperty, Object> property : properties.entrySet()) {
			propertiesNBT.putString(property.getKey().id.toString(), property.getKey().toString(property.getValue()));
		}

		tag.put("properties", propertiesNBT);

		TeamSavedEvent.EVENT.invoker().accept(new TeamSavedEvent(this));
		tag.put("extra", extraData);

		return tag;
	}

	public void deserializeNBT(CompoundTag tag) {
		owner = UUIDTypeAdapter.fromString(tag.getString("owner"));

		members.clear();
		ListTag membersNBT = tag.getList("members", NbtType.STRING);

		for (int i = 0; i < membersNBT.size(); i++) {
			members.add(UUIDTypeAdapter.fromString(membersNBT.getString(i)));
		}

		properties.clear();
		CompoundTag propertiesNBT = tag.getCompound("properties");

		Map<String, TeamProperty> map = new HashMap<>();
		TeamConfigEvent.EVENT.invoker().accept(new TeamConfigEvent(p -> map.put(p.id.toString(), p)));

		for (String key : propertiesNBT.getAllKeys()) {
			TeamProperty property = map.get(key);

			if (property != null && property.isValidFor(this)) {
				Optional optional = property.fromString(propertiesNBT.getString(key));

				if (optional.isPresent()) {
					properties.put(property, optional.get());
				}
			}
		}

		extraData = tag.getCompound("extra");
		TeamLoadedEvent.EVENT.invoker().accept(new TeamLoadedEvent(this));
	}

	// Commands //

	/*

	@Deprecated
	public int delete(CommandSourceStack source) throws CommandSyntaxException {
		if (delete()) {
			source.sendSuccess(new TextComponent("Deleted " + getId()), true);
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int settings(CommandSourceStack source, TeamProperty key, String value) throws CommandSyntaxException {
		if (value.isEmpty()) {
			TextComponent keyc = new TextComponent(key.id.toString());
			keyc.withStyle(ChatFormatting.YELLOW);
			TextComponent valuec = new TextComponent(key.toString(getProperty(key)));
			valuec.withStyle(ChatFormatting.AQUA);
			source.sendSuccess(new TextComponent("").append(keyc).append(" is set to ").append(valuec), true);
		} else {
			Optional optional = key.fromString(value);

			if (!optional.isPresent()) {
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

	@Deprecated
	public int join(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Optional<Team> oldTeam = manager.getTeam(player);

		if (oldTeam.isPresent()) {
			throw TeamArgument.ALREADY_IN_TEAM.create();
		}

		if (!isInvited(player)) {
			throw TeamArgument.NOT_INVITED.create(getName());
		}

		getInvited().remove(player.getGameProfile());
		addMember(player);

		for (ServerPlayer member : getOnlineMembers()) {
			member.displayClientMessage(new TextComponent("").append(player.getDisplayName()).append(" joined ").append(getName()), false);
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int leave(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		List<ServerPlayer> onlineMembers = getOnlineMembers();

		if (removeMember(player.getGameProfile(), true)) {
			for (ServerPlayer member : onlineMembers) {
				member.displayClientMessage(new TextComponent("").append(player.getDisplayName()).append(" left ").append(getName()), false);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int invite(CommandSourceStack source, Collection<GameProfile> players) throws CommandSyntaxException {
		if (!getProperty(FREE_TO_JOIN) && invited.addAll(players)) {
			save();
		}

		for (GameProfile player : players) {
			ServerPlayer playerEntity = ProfileUtils.getPlayerByProfile(manager.getServer(), player);

			if (playerEntity != null) {
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
			} else {
				source.sendSuccess(new TextComponent("Invited " + player.getName() + " to ").append(getName()), true);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int denyInvite(CommandSourceStack source) throws CommandSyntaxException {
		if (invited.remove(source.getPlayerOrException().getGameProfile())) {
			source.sendSuccess(new TextComponent("Invite denied"), true);
			save();
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int kick(CommandSourceStack source, Collection<GameProfile> players) throws CommandSyntaxException {
		invited.removeAll(players);

		for (GameProfile player : players) {
			ServerPlayer playerEntity = ProfileUtils.getPlayerByProfile(manager.getServer(), player);

			if (playerEntity != null) {
				source.sendSuccess(new TextComponent("Kicked ").append(playerEntity.getDisplayName()).append(" from ").append(getName()), true);
				playerEntity.displayClientMessage(new TextComponent("You have been kicked from ").append(getName()).append("!"), false);
			} else {
				source.sendSuccess(new TextComponent("Kicked " + player.getName() + " from ").append(getName()), true);
			}

			removeMember(player, true);
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int transferOwnership(CommandSourceStack source, ServerPlayer to) throws CommandSyntaxException {
		ServerPlayer from = source.getPlayerOrException();

		if (!isMember(to)) {
			throw TeamArgument.NOT_MEMBER.create(to.getDisplayName(), getName());
		}

		owner = to.getGameProfile();
		save();
		PlayerTransferredTeamOwnershipEvent.EVENT.invoker().accept(new PlayerTransferredTeamOwnershipEvent(this, from, to));
		source.sendSuccess(new TextComponent("Transferred ownership to ").append(to.getDisplayName()), true);
		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int info(CommandSourceStack source) throws CommandSyntaxException {
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

		for (GameProfile member : getMembers()) {
			TextComponent memberComponent = new TextComponent("- " + member.getName());
			memberComponent.withStyle(ChatFormatting.YELLOW);
			source.sendSuccess(memberComponent, true);
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int msg(ServerPlayer player, String message) throws CommandSyntaxException {
		Component m = FTBTUtils.newChatWithLinks(message);
		TextComponent component = new TextComponent("<");
		component.append(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW));
		component.append(" @");
		component.append(getName());
		component.append("> ");
		component.append(m);
		messageHistory.add(new TeamMessage(player.getGameProfile(), Instant.now(), m));

		if (messageHistory.size() > 100) {
			messageHistory.remove(0);
		}

		for (ServerPlayer p : getOnlineMembers()) {
			p.displayClientMessage(component, false);
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int gui(CommandSourceStack source) throws CommandSyntaxException {
		openGUI(source.getPlayerOrException());
		return Command.SINGLE_SUCCESS;
	}

	 */

	public void openGUI(ServerPlayer player) {
		// new MessageOpenGUI(messageHistory).sendTo(player);
	}
}