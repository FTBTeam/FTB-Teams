package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.Team;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class PlayerJoinedTeamEvent extends TeamEvent
{
	private final Optional<Team> previousTeam;
	private final ServerPlayerEntity player;

	public PlayerJoinedTeamEvent(Team t, Optional<Team> t0, ServerPlayerEntity p)
	{
		super(t);
		previousTeam = t0;
		player = p;
	}

	public Optional<Team> getPreviousTeam()
	{
		return previousTeam;
	}

	public ServerPlayerEntity getPlayer()
	{
		return player;
	}
}