package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamDeletedEvent extends TeamEvent {
	public static final Event<Consumer<TeamDeletedEvent>> EVENT = EventFactory.createConsumerLoop(TeamDeletedEvent.class);

	public TeamDeletedEvent(Team t) {
		super(t);
	}
}