package dev.ftb.mods.ftbteams.api.event;

import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired server-side when a new team is created; could be a player, party, or server team.
 */
public class TeamCreatedEvent extends TeamEvent {
	private final ServerPlayer creator;

	public TeamCreatedEvent(Team team, ServerPlayer creator) {
		super(team);
		this.creator = creator;
	}

	/**
	 * Get the player responsible for creation of the team.
	 *
	 * @return the creating player
	 */
	public ServerPlayer getCreator() {
		return creator;
	}
}