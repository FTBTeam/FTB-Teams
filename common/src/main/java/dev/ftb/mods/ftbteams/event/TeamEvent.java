package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;

/**
 * @author LatvianModder
 */
public class TeamEvent {
	private final Team team;

	public TeamEvent(Team t) {
		team = t;
	}

	public Team getTeam() {
		return team;
	}
}