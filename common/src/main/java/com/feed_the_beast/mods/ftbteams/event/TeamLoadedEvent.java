package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.data.Team;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamLoadedEvent extends TeamEvent {
	public static final Event<Consumer<TeamLoadedEvent>> EVENT = EventFactory.createConsumerLoop(TeamLoadedEvent.class);

	public TeamLoadedEvent(Team t) {
		super(t);
	}

	public CompoundTag getExtraData() {
		return getTeam().getExtraData();
	}
}