package dev.ftb.mods.ftbteams.data;

import java.util.UUID;

public class ClientTeam extends TeamBase {
	public final ClientTeamManager manager;
	TeamType type;

	public ClientTeam(ClientTeamManager m, UUID i) {
		manager = m;
		type = TeamType.PLAYER;
		id = i;
	}

	@Override
	public TeamType getType() {
		return type;
	}
}
