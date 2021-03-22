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
	private final CompoundTag extra;

	public TeamLoadedEvent(Team t, CompoundTag e) {
		super(t);
		extra = e;
	}

	public CompoundTag getExtraData() {
		return extra;
	}
}