package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.TeamProperty;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamConfigEvent extends FTBTeamsEvent
{
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