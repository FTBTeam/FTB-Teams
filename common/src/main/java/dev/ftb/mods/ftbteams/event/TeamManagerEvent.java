package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.TeamManager;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamManagerEvent {
	public static final Event<Consumer<TeamManagerEvent>> CREATED = EventFactory.createConsumerLoop(TeamManagerEvent.class);
	public static final Event<Consumer<TeamManagerEvent>> LOADED = EventFactory.createConsumerLoop(TeamManagerEvent.class);
	public static final Event<Consumer<TeamManagerEvent>> SAVED = EventFactory.createConsumerLoop(TeamManagerEvent.class);
	public static final Event<Consumer<TeamManagerEvent>> DESTROYED = EventFactory.createConsumerLoop(TeamManagerEvent.class);

	private final TeamManager manager;

	public TeamManagerEvent(TeamManager t) {
		manager = t;
	}

	public TeamManager getManager() {
		return manager;
	}

	public CompoundTag getExtraData() {
		return manager.getExtraData();
	}
}