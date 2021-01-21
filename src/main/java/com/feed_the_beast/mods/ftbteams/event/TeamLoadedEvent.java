package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.api.Team;
import net.minecraft.nbt.CompoundTag;

/**
 * @author LatvianModder
 */
public class TeamLoadedEvent extends TeamEvent
{
	private final CompoundTag extra;

	public TeamLoadedEvent(Team t, CompoundTag e)
	{
		super(t);
		extra = e;
	}

	public CompoundTag getExtraData()
	{
		return extra;
	}
}