package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.property.TeamProperties;

/**
 * @author LatvianModder
 */
public class ClientTeamPropertiesChangedEvent {
	private final ClientTeam team;
	private final TeamProperties old;

	public ClientTeamPropertiesChangedEvent(ClientTeam t, TeamProperties p) {
		team = t;
		old = p;
	}

	public ClientTeam getTeam() {
		return team;
	}

	public TeamProperties getOldProperties() {
		return old;
	}
}