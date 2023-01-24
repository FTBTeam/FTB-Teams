package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.net.UpdatePresenceMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

	public void createParty(ServerPlayer player, String name, String description, int color, Set<GameProfile> invited) {
		try {
			PartyTeam team = manager.createParty(player, name, description, Color4I.rgb(color)).getRight();
			team.invite(player, invited);
		} catch (CommandSyntaxException ex) {
			player.displayClientMessage(Component.literal(ex.getMessage()).withStyle(ChatFormatting.RED), false);
		}
	}

	public boolean hasTeam() {
		return actualTeam != this;
	}
}