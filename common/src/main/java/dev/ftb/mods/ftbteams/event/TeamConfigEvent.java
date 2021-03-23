package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.TeamProperty;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamConfigEvent {
	public static final Event<Consumer<TeamConfigEvent>> EVENT = EventFactory.createConsumerLoop(TeamConfigEvent.class);

	private final Consumer<TeamProperty> callback;

	public TeamConfigEvent(Consumer<TeamProperty> c) {
		callback = c;
	}

	public void add(TeamProperty property) {
		callback.accept(property);
	}
}