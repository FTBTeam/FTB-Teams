package dev.ftb.mods.ftbteams.data;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.util.TextComponentUtils;
import dev.ftb.mods.ftbteams.event.*;
import dev.ftb.mods.ftbteams.net.SendMessageResponseMessage;
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

		ListTag messageHistoryTag = tag.getList("message_history", NbtType.COMPOUND);

		for (int i = 0; i < messageHistoryTag.size(); i++) {
			CompoundTag mt = messageHistoryTag.getCompound(i);
			addMessage(new TeamMessage(UUID.fromString(mt.getString("from")), mt.getLong("date"), Component.Serializer.fromJson(mt.getString("text"))));
		}

		TeamEvent.LOADED.invoker().accept(new TeamEvent(this));
	}

	public <T> int settings(CommandSourceStack source, TeamProperty<T> key, String value) throws CommandSyntaxException {
		if (value.isEmpty()) {
			BaseComponent keyc = new TranslatableComponent("ftbteamsconfig." + key.id.getNamespace() + "." + key.id.getPath());
			keyc.withStyle(ChatFormatting.YELLOW);
			TextComponent valuec = new TextComponent(key.toString(getProperty(key)));
			valuec.withStyle(ChatFormatting.AQUA);
			source.sendSuccess(new TextComponent("").append(keyc).append(" is set to ").append(valuec), true);
		} else {
			Optional<T> optional = key.fromString(value);

			if (!optional.isPresent()) {
				//throw CommandSyntaxException
				source.sendFailure(new TextComponent("Failed to parse value!"));
				return 0;
			}

			TeamProperties old = properties.copy();

			setProperty(key, optional.get());
			BaseComponent keyc = new TranslatableComponent("ftbteamsconfig." + key.id.getNamespace() + "." + key.id.getPath());
			keyc.withStyle(ChatFormatting.YELLOW);
			TextComponent valuec = new TextComponent(value);
			valuec.withStyle(ChatFormatting.AQUA);
			source.sendSuccess(new TextComponent("Set ").append(keyc).append(" to ").append(valuec), true);
			TeamEvent.PROPERTIES_CHANGED.invoker().accept(new TeamPropertiesChangedEvent(this, old));
		}

		return Command.SINGLE_SUCCESS;
	}

	public int denyInvite(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		if (isInvited(player.getUUID()) && !isMember(player.getUUID())) {
			ranks.put(player.getUUID(), TeamRank.ALLY);
			source.sendSuccess(new TranslatableComponent("ftbteams.message.declined"), true);
			save();
			manager.syncTeamsToAll(this);
		}

		return Command.SINGLE_SUCCESS;
	}

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

		TextComponent component = new TextComponent("<");
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