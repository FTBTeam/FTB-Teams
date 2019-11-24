package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.Team;
import net.minecraft.entity.player.ServerPlayerEntity;

/**
 * @author LatvianModder
 */
public class PlayerTransferredTeamOwnershipEvent extends TeamEvent
{
	private final ServerPlayerEntity from, to;

	public PlayerTransferredTeamOwnershipEvent(Team t, ServerPlayerEntity pf, ServerPlayerEntity pt)
	{
		super(t);
		from = pf;
		to = pt;
	}

	public ServerPlayerEntity getFrom()
	{
		return from;
	}

	public ServerPlayerEntity getTo()
	{
		return to;
	}
}