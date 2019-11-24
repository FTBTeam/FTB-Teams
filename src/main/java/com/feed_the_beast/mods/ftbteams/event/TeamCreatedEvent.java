package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.Team;

/**
 * @author LatvianModder
 */
public class TeamCreatedEvent extends TeamEvent
{
	public TeamCreatedEvent(Team t)
	{
		super(t);
	}
}