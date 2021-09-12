package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.net.UpdatePresenceMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerTeam extends Team {
	public String playerName;
	public boolean online;
	public Team actualTeam;

	public PlayerTeam(TeamManager m) {
		super(m);
		playerName = "";
		online = false;
		actualTeam = this;
	}

	@Override
	public TeamType getType() {
		return TeamType.PLAYER;
	}

	@Override
	protected void serializeExtraNBT(CompoundTag tag) {
		tag.putString("player_name", playerName);
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		super.deserializeNBT(tag);
		playerName = tag.getString("player_name");
	}

	@Nullable
	public ServerPlayer getPlayer() {
		return FTBTUtils.getPlayerByUUID(manager.server, id);
	}

	@Override
	public TeamRank getHighestRank(UUID playerId) {
		if (playerId.equals(id)) {
			return TeamRank.OWNER;
		}

		return super.getHighestRank(playerId);
	}

	@Override
	public List<ServerPlayer> getOnlineMembers() {
		ServerPlayer p = getPlayer();
		return p == null ? Collections.emptyList() : Collections.singletonList(p);
	}

	public void updatePresence() {
		new UpdatePresenceMessage(new KnownClientPlayer(this)).sendToAll(manager.server);
	}

	public void createParty(ServerPlayer player, String name, String description, int color, Set<UUID> invited) {
		try {
			PartyTeam team = manager.createParty(player, name).getRight();
			team.setProperty(DESCRIPTION, description);
			team.setProperty(COLOR, Color4I.rgb(color));

			manager.syncAll();

			for (UUID i : invited) {
				PlayerTeam it = manager.getInternalPlayerTeam(i);

				if (i.equals(it.actualTeam.id)) {
					ServerPlayer p = it.getPlayer();

					if (p != null) {
						p.sendMessage(new TextComponent("").append(player.getName()).append(" has invited you to join their party!"), Util.NIL_UUID);
						Component acceptButton = new TextComponent("Accept ✔").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party join " + team.getStringID())));
						Component denyButton = new TextComponent("Deny X").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party deny_invite " + team.getStringID())));
						p.sendMessage(new TextComponent("[").append(acceptButton).append("] [").append(denyButton).append("]"), Util.NIL_UUID);
					}
				}
			}
		} catch (Exception ex) {
		}
	}
}