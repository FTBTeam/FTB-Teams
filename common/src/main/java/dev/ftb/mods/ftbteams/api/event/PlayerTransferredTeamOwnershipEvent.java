package dev.ftb.mods.ftbteams.api.event;

import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class PlayerTransferredTeamOwnershipEvent extends TeamEvent {
	private final ServerPlayer from, to;

	public PlayerTransferredTeamOwnershipEvent(Team team, ServerPlayer prevOwner, ServerPlayer newOwner) {
		super(team);
		from = prevOwner;
		to = newOwner;
	}

	public ServerPlayer getFrom() {
		return from;
	}

	public ServerPlayer getTo() {
		return to;
	}
}