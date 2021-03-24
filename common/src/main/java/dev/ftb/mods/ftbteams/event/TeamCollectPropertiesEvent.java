package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.property.TeamProperty;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamCollectPropertiesEvent {
	public static final Event<Consumer<TeamCollectPropertiesEvent>> EVENT = EventFactory.createConsumerLoop(TeamCollectPropertiesEvent.class);

	private final Consumer<TeamProperty> callback;

	public TeamCollectPropertiesEvent(Consumer<TeamProperty> c) {
		callback = c;
	}

	public void add(TeamProperty property) {
		callback.accept(property);
	}
}