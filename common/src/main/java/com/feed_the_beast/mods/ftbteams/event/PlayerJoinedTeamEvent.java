package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.Team;
import java.util.Optional;
import java.util.function.Consumer;

import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class PlayerJoinedTeamEvent extends TeamEvent
{
	public static final Event<Consumer<PlayerJoinedTeamEvent>> EVENT = EventFactory.createConsumerLoop(PlayerJoinedTeamEvent.class);
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