package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.Team;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class PlayerJoinedTeamEvent extends TeamEvent
{
	private final Optional<Team> previousTeam;
	private final ServerPlayer player;

	public PlayerJoinedTeamEvent(Team t, Optional<Team> t0, ServerPlayer p)
	{
		super(t);
		previousTeam = t0;
		player = p;
	}

	public Optional<Team> getPreviousTeam()
	{
		return previousTeam;
	}

	public ServerPlayer getPlayer()
	{
		return player;
	}
}