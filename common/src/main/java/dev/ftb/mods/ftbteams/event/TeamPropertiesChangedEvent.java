package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamPropertiesChangedEvent extends TeamEvent {
	public static final Event<Consumer<TeamPropertiesChangedEvent>> EVENT = EventFactory.createConsumerLoop(TeamPropertiesChangedEvent.class);

	private final TeamProperties old;

	public TeamPropertiesChangedEvent(Team t, TeamProperties p) {
		super(t);
		old = p;
	}

	public TeamProperties getOldProperties() {
		return old;
	}
}