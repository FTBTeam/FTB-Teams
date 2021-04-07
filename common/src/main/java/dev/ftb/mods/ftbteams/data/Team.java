package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbteams.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.event.TeamCreatedEvent;
import dev.ftb.mods.ftbteams.event.TeamLoadedEvent;
import dev.ftb.mods.ftbteams.event.TeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.event.TeamSavedEvent;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import dev.ftb.mods.ftbteams.property.TeamProperty;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public abstract class Team extends TeamBase {
	public final TeamManager manager;
	boolean shouldSave;

	public Team(TeamManager m) {
		id = Util.NIL_UUID;
		manager = m;
		properties.collect();
	}

	@Override
	public void save() {
		shouldSave = true;
		manager.nameMap = null;
	}

	/*
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

		Set<UUID> prevMembers = new HashSet<>();

		if (!owner.equals(Util.NIL_UUID)) {
			prevMembers.add(owner);
		}

		for (Map.Entry<UUID, TeamRank> entry : ranks.entrySet()) {
			if (entry.getValue().is(TeamRank.MEMBER)) {
				prevMembers.add(entry.getKey());
			}
		}

		for (UUID id : prevMembers) {
			removeMember(id, false);
		}

		TeamDeletedEvent.EVENT.invoker().accept(new TeamDeletedEvent(this, prevMembers));
		manager.teamMap.remove(id);
		directory.resolve(getId() + ".nbt").toFile().delete();
		return true;
	}
	 */

	public List<ServerPlayer> getOnlineRanked(TeamRank rank) {
		List<ServerPlayer> list = new ArrayList<>();

		for (UUID id : getRanked(rank).keySet()) {
			ServerPlayer player = FTBTUtils.getPlayerByUUID(manager.server, id);

			if (player != null) {
				list.add(player);
			}
		}

		return list;
	}

	public List<ServerPlayer> getOnlineMembers() {
		return getOnlineRanked(TeamRank.MEMBER);
	}

	void created(ServerPlayer p) {
		TeamCreatedEvent.EVENT.invoker().accept(new TeamCreatedEvent(this, p));
		save();
		manager.save();
	}

	public void changedTeam(Optional<Team> prev, UUID player) {
		PlayerChangedTeamEvent.EVENT.invoker().accept(new PlayerChangedTeamEvent(this, prev, player));
	}

	// Data IO //

	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", UUIDTypeAdapter.fromUUID(getId()));
		tag.putString("type", getType().getSerializedName());

		CompoundTag ranksNBT = new CompoundTag();

		for (Map.Entry<UUID, TeamRank> entry : ranks.entrySet()) {
			ranksNBT.putString(UUIDTypeAdapter.fromUUID(entry.getKey()), entry.getValue().getSerializedName());
		}

		tag.put("ranks", ranksNBT);
		tag.put("properties", properties.write(new CompoundTag()));

		TeamSavedEvent.EVENT.invoker().accept(new TeamSavedEvent(this));
		tag.put("extra", extraData);

		return tag;
	}

	public void deserializeNBT(CompoundTag tag) {
		ranks.clear();
		CompoundTag ranksNBT = tag.getCompound("ranks");

		for (String s : ranksNBT.getAllKeys()) {
			ranks.put(UUIDTypeAdapter.fromString(s), TeamRank.NAME_MAP.get(ranksNBT.getString(s)));
		}

		properties.read(tag.getCompound("properties"));
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

	*/

	public int settings(CommandSourceStack source, TeamProperty key, String value) throws CommandSyntaxException {
		if (value.isEmpty()) {
			BaseComponent keyc = new TranslatableComponent("ftbteamsconfig." + key.id.getNamespace() + "." + key.id.getPath());
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

			TeamProperties old = properties.copy();

			setProperty(key, optional.get());
			BaseComponent keyc = new TranslatableComponent("ftbteamsconfig." + key.id.getNamespace() + "." + key.id.getPath());
			keyc.withStyle(ChatFormatting.YELLOW);
			TextComponent valuec = new TextComponent(value);
			valuec.withStyle(ChatFormatting.AQUA);
			source.sendSuccess(new TextComponent("Set ").append(keyc).append(" to ").append(valuec), true);
			TeamPropertiesChangedEvent.EVENT.invoker().accept(new TeamPropertiesChangedEvent(this, old));
		}

		return Command.SINGLE_SUCCESS;
	}

	/*

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

	public void sendMessage(GameProfile from, Component text) {
		messageHistory.add(new TeamMessage(from, System.currentTimeMillis(), text));

		if (messageHistory.size() > 1000) {
			messageHistory.remove(0);
		}

		TextComponent component = new TextComponent("<");
		component.append(from.equals(FTBTUtils.NO_PROFILE) ? new TextComponent("System").withStyle(ChatFormatting.LIGHT_PURPLE) : new TextComponent(from.getName()).withStyle(ChatFormatting.YELLOW));
		component.append(" @");
		component.append(getName());
		component.append("> ");
		component.append(text);

		for (ServerPlayer p : getOnlineMembers()) {
			p.displayClientMessage(component, false);
		}

		save();
	}
}