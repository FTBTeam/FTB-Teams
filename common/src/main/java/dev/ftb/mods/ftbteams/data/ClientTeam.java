package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftbteams.property.TeamProperties;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientTeam extends TeamBase {
	public final ClientTeamManager manager;
	TeamType type;
	private final UUID id;
	public final TeamProperties properties;
	final Map<UUID, TeamRank> ranks;

	public ClientTeam(ClientTeamManager m, UUID i) {
		manager = m;
		type = TeamType.PLAYER;
		id = i;
		properties = new TeamProperties();
		ranks = new HashMap<>();
	}

	@Override
	public TeamType getType() {
		return type;
	}

	public UUID getId() {
		return id;
	}

	public CompoundTag getExtraData() {
		return new CompoundTag();
	}

	public String getDisplayName() {
		return properties.get(Team.DISPLAY_NAME);
	}

	public int getColor() {
		return properties.get(Team.COLOR).rgb();
	}

	public String getStringID() {
		String s = getDisplayName().replaceAll("\\W", "");
		return (s.length() > 50 ? s.substring(0, 50) : s) + "#" + getId();
	}
}
