package dev.ftb.mods.ftbteams.data;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbteams.event.PlayerChangedTeamEvent;
import dev.ftb.mods.ftbteams.event.TeamCreatedEvent;
import dev.ftb.mods.ftbteams.event.TeamLoadedEvent;
import dev.ftb.mods.ftbteams.event.TeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.event.TeamSavedEvent;
import dev.ftb.mods.ftbteams.net.MessageSendMessageResponse;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import dev.ftb.mods.ftbteams.property.TeamProperty;
import me.shedaniel.architectury.utils.NbtType;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

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

	void updateCommands(ServerPlayer player) {
		manager.server.getPlayerList().sendPlayerPermissionLevel(player);
	}

	public void changedTeam(@Nullable Team prev, UUID player, @Nullable ServerPlayer p) {
		PlayerChangedTeamEvent.EVENT.invoker().accept(new PlayerChangedTeamEvent(this, prev, player, p));

		if (p != null) {
			updateCommands(p);
		}
	}

	public void changedTeam(@Nullable Team prev, UUID player) {
		changedTeam(prev, player, FTBTUtils.getPlayerByUUID(manager.server, player));
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

		ListTag messageHistoryTag = new ListTag();

		for (TeamMessage m : messageHistory) {
			CompoundTag mt = new CompoundTag();
			mt.putString("from", UUIDTypeAdapter.fromUUID(m.sender));
			mt.putLong("date", m.date);
			mt.putString("text", Component.Serializer.toJson(m.text));
			messageHistoryTag.add(mt);
		}

		tag.put("message_history", messageHistoryTag);

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
		messageHistory.clear();

		ListTag messageHistoryTag = tag.getList("message_history", NbtType.COMPOUND);

		for (int i = 0; i < messageHistoryTag.size(); i++) {
			CompoundTag mt = messageHistoryTag.getCompound(i);
			messageHistory.add(new TeamMessage(UUIDTypeAdapter.fromString(mt.getString("from")), mt.getLong("date"), Component.Serializer.fromJson(mt.getString("text"))));
		}

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

	@Deprecated
	public int denyInvite(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		if (isInvited(player.getUUID()) && !isMember(player.getUUID())) {
			ranks.put(player.getUUID(), TeamRank.ALLY);
			source.sendSuccess(new TextComponent("Invite denied"), true);
			save();
			manager.syncAll();
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int info(CommandSourceStack source) throws CommandSyntaxException {
		source.sendSuccess(TextComponent.EMPTY, false);

		TextComponent infoComponent = new TextComponent("");
		infoComponent.getStyle().withBold(true);
		infoComponent.append("== ");
		infoComponent.append(getName());
		infoComponent.append(" ==");
		source.sendSuccess(infoComponent, false);

		source.sendSuccess(new TranslatableComponent("ftbteams.info.id", new TextComponent(getId().toString()).withStyle(ChatFormatting.YELLOW)), false);
		source.sendSuccess(new TranslatableComponent("ftbteams.info.short_id", new TextComponent(getStringID()).withStyle(ChatFormatting.YELLOW)).append(" [" + getType().getSerializedName() + "]"), false);

		if (getOwner().equals(Util.NIL_UUID)) {
			source.sendSuccess(new TranslatableComponent("ftbteams.info.owner", new TranslatableComponent("ftbteams.info.owner.none")), false);
		} else {
			source.sendSuccess(new TranslatableComponent("ftbteams.info.owner", manager.getName(getOwner())), false);
		}

		source.sendSuccess(new TranslatableComponent("ftbteams.info.members"), false);

		if (getMembers().isEmpty()) {
			source.sendSuccess(new TextComponent("- ").append(new TranslatableComponent("ftbteams.info.members.none")), false);
		} else {
			for (UUID member : getMembers()) {
				source.sendSuccess(new TextComponent("- ").append(manager.getName(member)), false);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	public UUID getOwner() {
		return Util.NIL_UUID;
	}

	public int msg(ServerPlayer player, String message) throws CommandSyntaxException {
		sendMessage(player.getUUID(), FTBTUtils.newChatWithLinks(message));
		return Command.SINGLE_SUCCESS;
	}

	public void sendMessage(UUID from, Component text) {
		messageHistory.add(new TeamMessage(from, System.currentTimeMillis(), text));

		if (messageHistory.size() > 1000) {
			messageHistory.remove(0);
		}

		TextComponent component = new TextComponent("<");
		component.append(manager.getName(from));
		component.append(" @");
		component.append(getName());
		component.append("> ");
		component.append(text);

		for (ServerPlayer p : getOnlineMembers()) {
			p.displayClientMessage(component, false);
			new MessageSendMessageResponse(from, text).sendTo(p);
		}

		save();
	}
}