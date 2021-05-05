package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class PlayerJoinedPartyTeamEvent extends TeamEvent {
	private final PlayerTeam previousTeam;
	private final ServerPlayer player;

	public PlayerJoinedPartyTeamEvent(Team t, PlayerTeam o, ServerPlayer p) {
		super(t);
		previousTeam = o;
		player = p;
	}

	public PlayerTeam getPreviousTeam() {
		return previousTeam;
	}

	public ServerPlayer getPlayer() {
		return player;
	}
}