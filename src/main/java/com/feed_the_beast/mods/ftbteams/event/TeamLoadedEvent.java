package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.Team;
import net.minecraft.nbt.CompoundNBT;

/**
 * @author LatvianModder
 */
public class TeamLoadedEvent extends TeamEvent
{
	private final CompoundNBT extra;

	public TeamLoadedEvent(Team t, CompoundNBT e)
	{
		super(t);
		extra = e;
	}

	public CompoundNBT getExtraData()
	{
		return extra;
	}
}