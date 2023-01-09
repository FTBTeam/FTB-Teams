package dev.ftb.mods.ftbteams.data;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.util.TextComponentUtils;
import dev.ftb.mods.ftbteams.event.*;
import dev.ftb.mods.ftbteams.net.SendMessageResponseMessage;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import dev.ftb.mods.ftbteams.property.TeamProperty;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
	public boolean isValid() {
		return manager.teamMap.containsKey(id);
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

	void onCreated(@Nullable ServerPlayer p) {
		if (p != null) {
			TeamEvent.CREATED.invoker().accept(new TeamCreatedEvent(this, p));
		}
		save();
		manager.save();
	}

	void updateCommands(ServerPlayer player) {
		manager.server.getPlayerList().sendPlayerPermissionLevel(player);
	}

	void changedTeam(@Nullable Team prev, UUID player, @Nullable ServerPlayer p, boolean deleted) {
		TeamEvent.PLAYER_CHANGED.invoker().accept(new PlayerChangedTeamEvent(this, prev, player, p));

		if (prev instanceof PartyTeam) {
			TeamEvent.PLAYER_LEFT_PARTY.invoker().accept(new PlayerLeftPartyTeamEvent(prev, (PlayerTeam) this, player, p, deleted));
		} else if (prev instanceof PlayerTeam && p != null) {
			TeamEvent.PLAYER_JOINED_PARTY.invoker().accept(new PlayerJoinedPartyTeamEvent(this, (PlayerTeam) prev, p));
		}

		if (deleted && prev != null) {
			TeamEvent.DELETED.invoker().accept(new TeamEvent(prev));
		}

		if (p != null) {
			updateCommands(p);
		}
	}

	// Data IO //

	public SNBTCompoundTag serializeNBT() {
		SNBTCompoundTag tag = new SNBTCompoundTag();
		tag.putString("id", getId().toString());
		tag.putString("type", getType().getSerializedName());
		serializeExtraNBT(tag);

		SNBTCompoundTag ranksNBT = new SNBTCompoundTag();

		for (Map.Entry<UUID, TeamRank> entry : ranks.entrySet()) {
			ranksNBT.putString(entry.getKey().toString(), entry.getValue().getSerializedName());
		}

		tag.put("ranks", ranksNBT);
		tag.put("properties", properties.write(new SNBTCompoundTag()));

		ListTag messageHistoryTag = new ListTag();

		for (TeamMessage m : getMessageHistory()) {
			SNBTCompoundTag mt = new SNBTCompoundTag();
			mt.singleLine();
			mt.putString("from", m.sender.toString());
			mt.putLong("date", m.date);
			mt.putString("text", Component.Serializer.toJson(m.text));
			messageHistoryTag.add(mt);
		}

		tag.put("message_history", messageHistoryTag);

		TeamEvent.SAVED.invoker().accept(new TeamEvent(this));
		tag.put("extra", extraData);

		return tag;
	}

	protected void serializeExtraNBT(CompoundTag tag) {
	}

	public void deserializeNBT(CompoundTag tag) {
		ranks.clear();
		CompoundTag ranksNBT = tag.getCompound("ranks");

		for (String s : ranksNBT.getAllKeys()) {
			ranks.put(UUID.fromString(s), TeamRank.NAME_MAP.get(ranksNBT.getString(s)));
		}

		properties.read(tag.getCompound("properties"));
		extraData = tag.getCompound("extra");
		messageHistory.clear();

		ListTag messageHistoryTag = tag.getList("message_history", Tag.TAG_COMPOUND);

		for (int i = 0; i < messageHistoryTag.size(); i++) {
			CompoundTag mt = messageHistoryTag.getCompound(i);
			addMessage(new TeamMessage(UUID.fromString(mt.getString("from")), mt.getLong("date"), Component.Serializer.fromJson(mt.getString("text"))));
		}

		TeamEvent.LOADED.invoker().accept(new TeamEvent(this));
	}

	// Commands //

	/*

	@Deprecated
	public int delete(CommandSourceStack source) throws CommandSyntaxException {
		if (delete()) {
			source.sendSuccess(Component.literal("Deleted " + getId()), true);
		}

		return Command.SINGLE_SUCCESS;
	}

	*/

	public int settings(CommandSourceStack source, TeamProperty key, String value) throws CommandSyntaxException {
		if (value.isEmpty()) {
			MutableComponent keyc = Component.translatable("ftbteamsconfig." + key.id.getNamespace() + "." + key.id.getPath());
			keyc.withStyle(ChatFormatting.YELLOW);
			MutableComponent valuec = Component.literal(key.toString(getProperty(key)));
			valuec.withStyle(ChatFormatting.AQUA);
			source.sendSuccess(Component.literal("").append(keyc).append(" is set to ").append(valuec), true);
		} else {
			Optional optional = key.fromString(value);

			if (optional.isEmpty()) {
				//throw CommandSyntaxException
				source.sendSuccess(Component.literal("Failed to parse value!"), true);
				return 0;
			}

			TeamProperties old = properties.copy();

			setProperty(key, optional.get());
			MutableComponent keyc = Component.translatable("ftbteamsconfig." + key.id.getNamespace() + "." + key.id.getPath());
			keyc.withStyle(ChatFormatting.YELLOW);
			MutableComponent valuec = Component.literal(value);
			valuec.withStyle(ChatFormatting.AQUA);
			source.sendSuccess(Component.literal("Set ").append(keyc).append(" to ").append(valuec), true);
			TeamEvent.PROPERTIES_CHANGED.invoker().accept(new TeamPropertiesChangedEvent(this, old));
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int denyInvite(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		if (isInvited(player.getUUID()) && !isMember(player.getUUID())) {
			ranks.put(player.getUUID(), TeamRank.ALLY);
			source.sendSuccess(Component.literal("Invite denied"), true);
			save();
			manager.syncAll();
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int info(CommandSourceStack source) throws CommandSyntaxException {
		source.sendSuccess(Component.empty(), false);

		MutableComponent infoComponent = Component.literal("");
		infoComponent.getStyle().withBold(true);
		infoComponent.append("== ");
		infoComponent.append(getName());
		infoComponent.append(" ==");
		source.sendSuccess(infoComponent, false);

		source.sendSuccess(Component.translatable("ftbteams.info.id", Component.literal(getId().toString()).withStyle(ChatFormatting.YELLOW)), false);
		source.sendSuccess(Component.translatable("ftbteams.info.short_id", Component.literal(getStringID()).withStyle(ChatFormatting.YELLOW)).append(" [" + getType().getSerializedName() + "]"), false);

		if (getOwner().equals(Util.NIL_UUID)) {
			source.sendSuccess(Component.translatable("ftbteams.info.owner", Component.translatable("ftbteams.info.owner.none")), false);
		} else {
			source.sendSuccess(Component.translatable("ftbteams.info.owner", manager.getName(getOwner())), false);
		}

		source.sendSuccess(Component.translatable("ftbteams.info.members"), false);

		if (getMembers().isEmpty()) {
			source.sendSuccess(Component.literal("- ").append(Component.translatable("ftbteams.info.members.none")), false);
		} else {
			for (UUID member : getMembers()) {
				source.sendSuccess(Component.literal("- ").append(manager.getName(member)), false);
			}
		}

		TeamEvent.INFO.invoker().accept(new TeamInfoEvent(this, source));
		return Command.SINGLE_SUCCESS;
	}

	public UUID getOwner() {
		return Util.NIL_UUID;
	}

	public int msg(ServerPlayer player, String message) throws CommandSyntaxException {
		sendMessage(player.getUUID(), TextComponentUtils.withLinks(message));
		return Command.SINGLE_SUCCESS;
	}

	public void sendMessage(UUID from, Component text) {
		addMessage(new TeamMessage(from, System.currentTimeMillis(), text));

		MutableComponent component = Component.literal("<");
		component.append(manager.getName(from));
		component.append(" @");
		component.append(getName());
		component.append("> ");
		component.append(text);

		for (ServerPlayer p : getOnlineMembers()) {
			p.displayClientMessage(component, false);
			new SendMessageResponseMessage(from, text).sendTo(p);
		}

		save();
	}
}
