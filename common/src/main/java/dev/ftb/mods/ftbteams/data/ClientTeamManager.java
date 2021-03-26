package dev.ftb.mods.ftbteams.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientTeamManager {
	public static ClientTeamManager INSTANCE;

	private final UUID id;
	public final Map<UUID, ClientTeam> teamMap;

	public ClientTeamManager(UUID i) {
		id = i;
		teamMap = new HashMap<>();
	}

	public UUID getId() {
		return id;
	}
}
