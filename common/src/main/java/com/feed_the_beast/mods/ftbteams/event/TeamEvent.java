package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.data.Team;

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