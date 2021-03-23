package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamSavedEvent extends TeamEvent {
	public static final Event<Consumer<TeamSavedEvent>> EVENT = EventFactory.createConsumerLoop(TeamSavedEvent.class);

	public TeamSavedEvent(Team t) {
		super(t);
	}

	public CompoundTag getExtraData() {
		return getTeam().getExtraData();
	}
}