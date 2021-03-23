package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamCreatedEvent extends TeamEvent {
	public static final Event<Consumer<TeamCreatedEvent>> EVENT = EventFactory.createConsumerLoop(TeamCreatedEvent.class);

	private final ServerPlayer creator;

	public TeamCreatedEvent(Team t, ServerPlayer p) {
		super(t);
		creator = p;
	}

	public ServerPlayer getCreator() {
		return creator;
	}
}