package dev.ftb.mods.ftbteams.data;

import net.minecraft.util.StringRepresentable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public enum TeamType implements StringRepresentable {
	PLAYER("player", PlayerTeam::new),
	PARTY("party", PartyTeam::new),
	SERVER("server", ServerTeam::new);

	public static final Map<String, TeamType> MAP = new HashMap<>();

	static {
		for (TeamType t : values()) {
			MAP.put(t.name, t);
		}
	}

	private final String name;
	public final Function<TeamManager, Team> factory;

	TeamType(String n, Function<TeamManager, Team> f) {
		name = n;
		factory = f;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public boolean isPlayer() {
		return this == PLAYER;
	}

	public boolean isParty() {
		return this == PARTY;
	}

	public boolean isServer() {
		return this == SERVER;
	}
}
