package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftbteams.property.TeamProperty;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientTeam extends TeamBase {
	private static List<TeamProperty> SYNCABLE_PROPS = List.of(DISPLAY_NAME, COLOR);

	public final ClientTeamManager manager;
	public boolean invalid;
	TeamType type;
	private final UUID ownerID;

	public static ClientTeam invalidTeam(ClientTeamManager m, Team team) {
		return new ClientTeam(m, team.getId());
	}

	private ClientTeam(ClientTeamManager m, UUID id) {
		super();
		this.id = id;
		manager = m;
		ownerID = Util.NIL_UUID;
		invalid = true;
		type = TeamType.PARTY;
	}

	public ClientTeam(ClientTeamManager m, FriendlyByteBuf buffer) {
		manager = m;
		id = buffer.readUUID();
		type = buffer.readEnum(TeamType.class);
		properties.read(buffer);

		int rs = buffer.readVarInt();
		for (int i = 0; i < rs; i++) {
			ranks.put(buffer.readUUID(), buffer.readEnum(TeamRank.class));
		}

		extraData = buffer.readNbt();

		ownerID = buffer.readBoolean() ? buffer.readUUID() : Util.NIL_UUID;

		invalid = buffer.readBoolean();
	}

	public ClientTeam(ClientTeamManager m, Team team) {
		manager = m;
		id = team.getId();
		type = team.getType();
		properties.updateFrom(team.properties);
		ranks.putAll(team.ranks);
		extraData = team.extraData == null ? null : team.extraData.copy();
		ownerID = team.getOwner();
	}

	@Override
	public TeamType getType() {
		return type;
	}

	@Override
	public boolean isValid() {
		return manager.teamMap.containsKey(id);
	}

	public void write(FriendlyByteBuf buffer, boolean writeAllProperties) {
		buffer.writeUUID(id);
		buffer.writeEnum(type);
		if (writeAllProperties) {
			properties.write(buffer);
		} else {
			properties.writeSyncableOnly(buffer, SYNCABLE_PROPS);
		}

		buffer.writeVarInt(ranks.size());

		for (Map.Entry<UUID, TeamRank> entry : ranks.entrySet()) {
			buffer.writeUUID(entry.getKey());
			buffer.writeEnum(entry.getValue());
		}

		buffer.writeNbt(extraData);

		boolean hasOwner = !ownerID.equals(Util.NIL_UUID);
		buffer.writeBoolean(hasOwner);
		if (hasOwner) buffer.writeUUID(ownerID);

		buffer.writeBoolean(invalid);
	}

	public boolean isSelf() {
		return this == manager.selfTeam;
	}

	public UUID getOwnerID() {
		return ownerID;
	}

	public void setMessageHistory(List<TeamMessage> messages) {
		messageHistory.clear();
		messageHistory.addAll(messages);
	}
}
