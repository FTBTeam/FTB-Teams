package dev.ftb.mods.ftbteams.data;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.marhali.json5.Json5Object;
import dev.ftb.mods.ftblibrary.json5.Json5Ops;
import dev.ftb.mods.ftblibrary.json5.Json5Util;
import dev.ftb.mods.ftblibrary.platform.event.NativeEventPosting;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import dev.ftb.mods.ftblibrary.util.TextComponentUtils;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.ScoreboardTeamHelper;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.event.*;
import dev.ftb.mods.ftbteams.api.property.TeamProperty;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import dev.ftb.mods.ftbteams.net.SendMessageResponseMessage;
import dev.ftb.mods.ftbteams.net.UpdatePropertiesResponseMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// Base class for server-side teams
public abstract class AbstractTeam extends AbstractTeamBase {
	protected final TeamManagerImpl manager;
	private boolean shouldSave;

	public AbstractTeam(TeamManagerImpl manager, UUID id) {
		super(id);
		this.manager = manager;

		properties.collectProperties();
	}

	@Override
	public void markDirty() {
		shouldSave = true;
		manager.nameMap = null;
	}

	public List<ServerPlayer> getOnlineRanked(TeamRank rank) {
		List<ServerPlayer> list = new ArrayList<>();

		for (UUID id : getPlayersByRank(rank).keySet()) {
			ServerPlayer player = FTBTUtils.getPlayerByUUID(manager.getServer(), id);
			if (player != null) {
				list.add(player);
			}
		}

		return list;
	}

	@Override
	public List<ServerPlayer> getOnlineMembers() {
		return getOnlineRanked(TeamRank.MEMBER);
	}

	void onCreated(@Nullable ServerPlayer player, UUID playerId) {
		if (player != null) {
			NativeEventPosting.INSTANCE.postEvent(new TeamCreatedEvent.Data(this, player, playerId));
		}
		markDirty();
		manager.markDirty();
		manager.saveNow();
	}

	void updateCommands(ServerPlayer player) {
		player.level().getServer().getPlayerList().sendPlayerPermissionLevel(player);
	}

	void onPlayerChangeTeam(@Nullable Team prev, UUID player, @Nullable ServerPlayer serverPlayer, boolean deleted) {
		NativeEventPosting.INSTANCE.postEvent(new PlayerChangedTeamEvent.Data(this, prev, player, serverPlayer));

		if (prev instanceof PartyTeam && this instanceof PlayerTeam) {
			NativeEventPosting.INSTANCE.postEvent(new PlayerLeftPartyTeamEvent.Data(prev, this, player, serverPlayer, deleted));
		} else if (prev instanceof PlayerTeam && serverPlayer != null) {
			NativeEventPosting.INSTANCE.postEvent(new PlayerJoinedPartyTeamEvent.Data(this, prev, serverPlayer));
		}

		if (deleted && prev != null) {
			NativeEventPosting.INSTANCE.postEvent(new TeamDeletedEvent.Data(prev));
		}

		if (serverPlayer != null) {
			updateCommands(serverPlayer);
		}
	}

	// Data IO //

	public Json5Object toJson() {
		Json5Object json = new Json5Object();
		json.add("id", UUIDUtil.STRING_CODEC.encodeStart(Json5Ops.INSTANCE, getId()).getOrThrow());
		json.addProperty("type", getType().getSerializedName());

		serializeExtraJson(json);

		Json5Object ranksNBT = new Json5Object();
		for (Map.Entry<UUID, TeamRank> entry : ranks.entrySet()) {
			ranksNBT.addProperty(entry.getKey().toString(), entry.getValue().getSerializedName());
		}

		json.add("ranks", ranksNBT);
		json.add("properties", properties.toJson(new Json5Object()));
		Json5Util.store(json, "message_history", TeamMessageImpl.LIST_CODEC, getMessageHistory());

		return json;
	}

	protected void serializeExtraJson(Json5Object tag) {
	}

	public void deserializeJson(@UnknownNullability Json5Object json, HolderLookup.Provider provider) {
		ranks.clear();
		Json5Util.getJson5Object(json, "ranks").ifPresent(ranksJson ->
				ranksJson.asMap().forEach((key, val) ->
						ranks.put(UUID.fromString(key), TeamRank.NAME_MAP.get(val.getAsString()))));

		Json5Util.getJson5Object(json, "properties").ifPresent(properties::read);

		messageHistory.clear();
		Json5Util.fetch(json, "message_history", TeamMessageImpl.LIST_CODEC).ifPresent(messageHistory::addAll);

		NativeEventPosting.INSTANCE.postEvent(new TeamLoadedEvent.Data(this));
	}

	public <T> int settings(CommandSourceStack source, TeamProperty<T> key, @Nullable String valueStr) {
		MutableComponent keyc = Component.translatable(key.getTranslationKey("ftbteamsconfig")).withStyle(ChatFormatting.YELLOW);
		if (valueStr == null) {
			source.sendSuccess(() -> keyc.append(" = ").append(formatValueStr(key.toString(getProperty(key)))), true);
		} else if (key.isPlayerEditable()) {
			String valueStrStripped = stripQuotes(valueStr);
			return key.fromString(valueStrStripped).map(value -> {
				TeamPropertyCollection old = properties.copy();
				setProperty(key, value);

				source.sendSuccess(() -> Component.translatable("ftbteams.message.set_property", keyc, formatValueStr(valueStrStripped)), true);

				NativeEventPosting.INSTANCE.postEvent(new TeamPropertiesChangedEvent.Data(this, old, false));

				syncOnePropertyToAll(source.getServer(), key, value);
				if (!key.shouldSyncToAll()) {
					syncOnePropertyToTeam(key, value);
				}

				ScoreboardTeamHelper.onPropertiesChanged(manager.getServer(), this);

				return Command.SINGLE_SUCCESS;
			}).orElseGet(() -> {
				source.sendFailure(Component.translatable("ftbteams.message.parse_failed", valueStrStripped));
				return 0;
			});
		} else {
			source.sendFailure(Component.translatable("ftbteams.message.property_not_editable", keyc));
		}
		return Command.SINGLE_SUCCESS;
	}

	private static Component formatValueStr(String valueStr) {
		return valueStr.isEmpty() ?
				Component.translatable("gui.none").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC) :
				Component.literal(valueStr).withStyle(ChatFormatting.AQUA);
	}

	private static String stripQuotes(String valueStr) {
		if (valueStr.length() >= 2 && (valueStr.startsWith("\"") && valueStr.endsWith("\"") || valueStr.startsWith("'") && valueStr.endsWith("'"))) {
			return valueStr.substring(1, valueStr.length() - 1);
		} else {
			return valueStr;
		}
	}

	public int declineInvitation(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		if (getRankForPlayer(player.getUUID()) == TeamRank.INVITED) {
			ranks.put(player.getUUID(), TeamRank.ALLY);
			source.sendSuccess(() -> Component.translatable("ftbteams.message.declined"), true);
			markDirty();
			manager.syncToAll(this);
			return Command.SINGLE_SUCCESS;
		} else {
			FTBTeams.LOGGER.warn("ignore invitation decline for player {} to team {} (not invited)", player.getUUID(), getId());
			return 0;
		}
	}

	@Override
	public List<Component> getTeamInfo() {
		List<Component> res = new ArrayList<>();

		res.add(Component.literal("== ")
				.append(getName())
				.append(Component.literal(" [" + getType().getSerializedName() + "]").withStyle(getType().getColor()))
				.append(" ==")
		);
		res.add(Component.translatable("ftbteams.info.id", FTBTUtils.makeCopyableComponent(getId().toString()).withStyle(ChatFormatting.YELLOW)));
		res.add(Component.translatable("ftbteams.info.short_id", FTBTUtils.makeCopyableComponent(getShortName()).withStyle(ChatFormatting.YELLOW)));

		if (isPartyTeam()) {
			res.add(getOwner().equals(Util.NIL_UUID) ?
					Component.translatable("ftbteams.info.owner", Component.translatable("ftbteams.info.owner.none").withStyle(ChatFormatting.GRAY)) :
					Component.translatable("ftbteams.info.owner", playerWithId(getOwner()))
			);

			res.add(Component.translatable("ftbteams.info.members"));
			if (getMembers().isEmpty()) {
				res.add(Component.literal("- ").append(Component.translatable("ftbteams.info.members.none")).withStyle(ChatFormatting.GRAY));
			} else {
				for (UUID member : getMembers()) {
					res.add(Component.literal("- ").append(playerWithId(member)));
				}
			}
		}

		return res;
	}

	private Component playerWithId(UUID member) {
		return manager.getPlayerName(member).copy().withStyle(Style.EMPTY
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(member.toString())))
		);
	}

	@Override
	public UUID getOwner() {
		return Util.NIL_UUID;
	}

	@Override
	public void sendMessage(UUID senderId, String message) {
		sendMessage(senderId, TextComponentUtils.withLinks(message));
	}

	@Override
	public void sendMessage(UUID from, Component text) {
		addMessage(FTBTeamsAPI.api().createMessage(from, text));

		MutableComponent component = Component.literal("<");
		component.append(manager.getPlayerName(from));
		component.append(" @");
		component.append(getName());
		component.append("> ");
		component.append(text);

		for (ServerPlayer sp : getOnlineMembers()) {
			sp.sendSystemMessage(component);
			Server2PlayNetworking.send(sp, new SendMessageResponseMessage(from, text));
		}

		markDirty();
	}

	public void updatePropertiesFrom(TeamPropertyCollection newProperties) {
		TeamPropertyCollection oldProperties = properties.copy();
		properties.updateFrom(newProperties);
		NativeEventPosting.INSTANCE.postEvent(new TeamPropertiesChangedEvent.Data(this, oldProperties, false));
		markDirty();

		ScoreboardTeamHelper.onPropertiesChanged(manager.getServer(), this);
	}

	void saveIfNeeded(Path directory) {
		if (shouldSave) {
			try {
				Json5Util.save(directory.resolve(getType().getSerializedName() + "/" + getId() + Json5Util.FILE_EXT), toJson());
				NativeEventPosting.INSTANCE.postEvent(new TeamSavedEvent.Data(this));
			} catch (IOException e) {
				FTBTeams.LOGGER.error("Failed to save team {}", getId(), e);
			}
			shouldSave = false;
		}
	}

	@Override
	public <T> void syncOnePropertyToAll(MinecraftServer server, TeamProperty<T> property, T value) {
		if (property.shouldSyncToAll()) {
			Server2PlayNetworking.sendToAllPlayers(server, UpdatePropertiesResponseMessage.oneProperty(getId(), property, value));
		}
	}

	@Override
	public <T> void syncOnePropertyToTeam(TeamProperty<T> property, T value) {
		getOnlineMembers().forEach(sp -> Server2PlayNetworking.send(sp, UpdatePropertiesResponseMessage.oneProperty(getId(), property, value)));
	}
}
