package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.data.Team;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamDeletedEvent extends TeamEvent {
	public static final Event<Consumer<TeamDeletedEvent>> EVENT = EventFactory.createConsumerLoop(TeamDeletedEvent.class);

	private final Set<UUID> members;

	public TeamDeletedEvent(Team t, Set<UUID> m) {
		super(t);
		members = m;
	}

	public Set<UUID> getMembers() {
		return members;
	}
}