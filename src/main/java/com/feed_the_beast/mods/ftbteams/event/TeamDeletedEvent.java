package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.Team;
import com.mojang.authlib.GameProfile;

import java.util.Set;

/**
 * @author LatvianModder
 */
public class TeamDeletedEvent extends TeamEvent
{
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