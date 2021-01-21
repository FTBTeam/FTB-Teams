package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.Team;
import com.mojang.authlib.GameProfile;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

import java.util.Set;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamDeletedEvent extends TeamEvent
{
	public static final Event<Consumer<TeamDeletedEvent>> EVENT = EventFactory.createConsumerLoop(TeamDeletedEvent.class);
	private final Set<GameProfile> members;

	public TeamDeletedEvent(Team t, Set<GameProfile> m)
	{
		super(t);
		members = m;
	}

	public Set<GameProfile> getMembers()
	{
		return members;
	}
}