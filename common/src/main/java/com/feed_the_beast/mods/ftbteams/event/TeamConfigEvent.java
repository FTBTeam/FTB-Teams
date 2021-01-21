package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.TeamProperty;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import me.shedaniel.architectury.event.EventHandler;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamConfigEvent extends FTBTeamsEvent
{
    public static final Event<Consumer<TeamConfigEvent>> EVENT = EventFactory.createConsumerLoop(TeamConfigEvent.class);
	private final Consumer<TeamProperty> callback;

	public TeamConfigEvent(Consumer<TeamProperty> c)
	{
		callback = c;
	}

	public void add(TeamProperty property)
	{
		callback.accept(property);
	}
}