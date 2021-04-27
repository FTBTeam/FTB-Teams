package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class ClientTeamPropertiesChangedEvent {
	public static final Event<Consumer<ClientTeamPropertiesChangedEvent>> EVENT = EventFactory.createConsumerLoop(ClientTeamPropertiesChangedEvent.class);

	private final ClientTeam team;
	private final TeamProperties old;

	public ClientTeamPropertiesChangedEvent(ClientTeam t, TeamProperties p) {
		team = t;
		old = p;
	}

	public ClientTeam getTeam() {
		return team;
	}

	public TeamProperties getOldProperties() {
		return old;
	}
}