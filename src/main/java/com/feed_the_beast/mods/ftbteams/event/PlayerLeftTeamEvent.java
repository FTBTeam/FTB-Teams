package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.Team;
import com.mojang.authlib.GameProfile;

/**
 * @author LatvianModder
 */
public class PlayerLeftTeamEvent extends TeamEvent
{
	private final GameProfile profile;

	public PlayerLeftTeamEvent(Team t, GameProfile pr)
	{
		super(t);
		profile = pr;
	}

	public GameProfile getProfile()
	{
		return profile;
	}
}