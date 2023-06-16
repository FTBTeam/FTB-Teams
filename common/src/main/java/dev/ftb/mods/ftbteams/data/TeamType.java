package dev.ftb.mods.ftbteams.data;

import net.minecraft.util.StringRepresentable;

import java.util.UUID;
import java.util.function.BiFunction;

public enum TeamType implements StringRepresentable {
	PLAYER("player", PlayerTeam::new),
	PARTY("party", PartyTeam::new),
	SERVER("server", ServerTeam::new);

	private final String name;
	private final BiFunction<TeamManagerImpl, UUID, AbstractTeam> factory;

	TeamType(String n, BiFunction<TeamManagerImpl, UUID, AbstractTeam> f) {
		name = n;
		factory = f;
	}

	public AbstractTeam createTeam(TeamManagerImpl manager, UUID id) {
		return factory.apply(manager, id);
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
