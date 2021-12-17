package dev.ftb.mods.ftbteams.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbteams.data.Team;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamEvent {
	public static final Event<Consumer<TeamCreatedEvent>> CREATED = EventFactory.createConsumerLoop();
	public static final Event<Consumer<TeamEvent>> LOADED = EventFactory.createConsumerLoop();
	public static final Event<Consumer<TeamEvent>> SAVED = EventFactory.createConsumerLoop();
	public static final Event<Consumer<TeamEvent>> DELETED = EventFactory.createConsumerLoop();
	public static final Event<Consumer<PlayerLoggedInAfterTeamEvent>> PLAYER_LOGGED_IN = EventFactory.createConsumerLoop();
	public static final Event<Consumer<PlayerTransferredTeamOwnershipEvent>> OWNERSHIP_TRANSFERRED = EventFactory.createConsumerLoop();
	public static final Event<Consumer<TeamCollectPropertiesEvent>> COLLECT_PROPERTIES = EventFactory.createConsumerLoop();
	public static final Event<Consumer<TeamPropertiesChangedEvent>> PROPERTIES_CHANGED = EventFactory.createConsumerLoop();
	public static final Event<Consumer<PlayerChangedTeamEvent>> PLAYER_CHANGED = EventFactory.createConsumerLoop();
	public static final Event<Consumer<PlayerJoinedPartyTeamEvent>> PLAYER_JOINED_PARTY = EventFactory.createConsumerLoop();
	public static final Event<Consumer<PlayerLeftPartyTeamEvent>> PLAYER_LEFT_PARTY = EventFactory.createConsumerLoop();
	public static final Event<Consumer<TeamInfoEvent>> INFO = EventFactory.createConsumerLoop();

	public static final Event<Consumer<ClientTeamPropertiesChangedEvent>> CLIENT_PROPERTIES_CHANGED = EventFactory.createConsumerLoop(ClientTeamPropertiesChangedEvent.class);

	private final Team team;

	public TeamEvent(Team t) {
		team = t;
	}

	public Team getTeam() {
		return team;
	}
}