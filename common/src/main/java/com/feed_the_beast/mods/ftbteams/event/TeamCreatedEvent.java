package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.data.Team;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamCreatedEvent extends TeamEvent {
	public static final Event<Consumer<TeamCreatedEvent>> EVENT = EventFactory.createConsumerLoop(TeamCreatedEvent.class);

	public TeamCreatedEvent(Team t) {
		super(t);
	}
}