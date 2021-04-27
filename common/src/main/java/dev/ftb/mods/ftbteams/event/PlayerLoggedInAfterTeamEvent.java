package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class PlayerLoggedInAfterTeamEvent extends TeamEvent {
	public static final Event<Consumer<PlayerLoggedInAfterTeamEvent>> EVENT = EventFactory.createConsumerLoop(PlayerLoggedInAfterTeamEvent.class);

	private final ServerPlayer player;

	public PlayerLoggedInAfterTeamEvent(Team t, ServerPlayer p) {
		super(t);
		player = p;
	}

	public ServerPlayer getPlayer() {
		return player;
	}
}