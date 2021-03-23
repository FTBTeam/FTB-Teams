package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class PlayerTransferredTeamOwnershipEvent extends TeamEvent {
	public static final Event<Consumer<PlayerTransferredTeamOwnershipEvent>> EVENT = EventFactory.createConsumerLoop(PlayerTransferredTeamOwnershipEvent.class);

	private final ServerPlayer from, to;

	public PlayerTransferredTeamOwnershipEvent(Team t, ServerPlayer pf, ServerPlayer pt) {
		super(t);
		from = pf;
		to = pt;
	}

	public ServerPlayer getFrom() {
		return from;
	}

	public ServerPlayer getTo() {
		return to;
	}
}