package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class TeamCreatedEvent extends TeamEvent {
	private final ServerPlayer creator;

	public TeamCreatedEvent(Team t, ServerPlayer p) {
		super(t);
		creator = p;
	}

	public ServerPlayer getCreator() {
		return creator;
	}
}