package dev.ftb.mods.ftbteams.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PlayerTeam extends Team {
	public String playerName;

	public PlayerTeam(TeamManager m) {
		super(m);
		playerName = "";
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
}
