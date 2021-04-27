package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.property.TeamProperties;

/**
 * @author LatvianModder
 */
public class TeamPropertiesChangedEvent extends TeamEvent {
	private final TeamProperties old;

	public TeamPropertiesChangedEvent(Team t, TeamProperties p) {
		super(t);
		old = p;
	}

	public TeamProperties getOldProperties() {
		return old;
	}
}