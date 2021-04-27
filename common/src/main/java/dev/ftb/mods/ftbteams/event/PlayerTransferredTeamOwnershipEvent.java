package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class PlayerTransferredTeamOwnershipEvent extends TeamEvent {
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