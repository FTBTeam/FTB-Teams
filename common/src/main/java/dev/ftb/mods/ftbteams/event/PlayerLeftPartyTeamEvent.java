package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class PlayerLeftPartyTeamEvent extends TeamEvent {
	private final PlayerTeam newTeam;
	private final UUID playerId;
	private final ServerPlayer player;
	private final boolean teamDeleted;

	public PlayerLeftPartyTeamEvent(Team t, PlayerTeam o, UUID pid, @Nullable ServerPlayer p, boolean d) {
		super(t);
		newTeam = o;
		playerId = pid;
		player = p;
		teamDeleted = d;
	}

	public PlayerTeam getNewTeam() {
		return newTeam;
	}

	public UUID getPlayerId() {
		return playerId;
	}

	@Nullable
	public ServerPlayer getPlayer() {
		return player;
	}

	public boolean getTeamDeleted() {
		return teamDeleted;
	}
}