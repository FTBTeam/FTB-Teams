package dev.ftb.mods.ftbteams.data;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;
import java.util.UUID;

public class ClientTeam extends TeamBase {
	public final ClientTeamManager manager;
	public boolean invalid;
	TeamType type;

	public ClientTeam(ClientTeamManager m, FriendlyByteBuf buffer, long now) {
		manager = m;
		id = buffer.readUUID();
		type = TeamType.VALUES[buffer.readByte()];
		properties.read(buffer);

		int rs = buffer.readVarInt();

		for (int i = 0; i < rs; i++) {
			ranks.put(buffer.readUUID(), TeamRank.VALUES[buffer.readByte()]);
		}

		extraData = buffer.readNbt();
	}

	public ClientTeam(ClientTeamManager m, Team team) {
		manager = m;
		id = team.getId();
		type = team.getType();
		properties.updateFrom(team.properties);
		ranks.putAll(team.ranks);
		extraData = team.extraData == null ? null : team.extraData.copy();
	}

	@Override
	public TeamType getType() {
		return type;
	}

	@Override
	public boolean isValid() {
		return manager.teamMap.containsKey(id);
	}

	public void write(FriendlyByteBuf buffer, long now) {
		buffer.writeUUID(id);
		buffer.writeByte(type.ordinal());
		properties.write(buffer);

		buffer.writeVarInt(ranks.size());

		for (Map.Entry<UUID, TeamRank> entry : ranks.entrySet()) {
			buffer.writeUUID(entry.getKey());
			buffer.writeByte(entry.getValue().ordinal());
		}

		buffer.writeNbt(extraData);
	}

	public boolean isSelf() {
		return this == manager.selfTeam;
	}
}
