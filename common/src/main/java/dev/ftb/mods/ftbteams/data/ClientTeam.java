package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftbteams.event.ClientTeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import dev.ftb.mods.ftbteams.property.TeamProperty;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientTeam extends TeamBase {
	private static final List<TeamProperty> SYNCABLE_PROPS = List.of(DISPLAY_NAME, COLOR);

	private final ClientTeamManager manager;
	private final TeamType type;
	private final UUID ownerID;
	private final boolean toBeRemoved;

	private ClientTeam(ClientTeamManager m, UUID id, UUID ownerId, TeamType type, boolean toBeRemoved) {
		super(id);
		this.manager = m;
		this.ownerID = ownerId;
		this.type = type;
		this.toBeRemoved = toBeRemoved;
	}

	public static ClientTeam invalidTeam(ClientTeamManager m, Team team) {
		return new ClientTeam(m, team.getId(), Util.NIL_UUID, team.getType(), true);
	}

	public static ClientTeam fromNetwork(ClientTeamManager manager, FriendlyByteBuf buffer) {
		UUID id = buffer.readUUID();
		UUID ownerID = buffer.readBoolean() ? buffer.readUUID() : Util.NIL_UUID;
		TeamType type = buffer.readEnum(TeamType.class);
		boolean mustRemove = buffer.readBoolean();

		ClientTeam clientTeam = new ClientTeam(manager, id, ownerID, type, mustRemove);

		clientTeam.properties.read(buffer);
		int nMembers = buffer.readVarInt();
		for (int i = 0; i < nMembers; i++) {
			clientTeam.addMember(buffer.readUUID(), buffer.readEnum(TeamRank.class));
		}

		clientTeam.extraData = buffer.readNbt();

		return clientTeam;
	}

	public static ClientTeam copyOf(ClientTeamManager manager, Team team) {
		ClientTeam clientTeam = new ClientTeam(manager, team.id, team.getOwner(), team.getType(), false);
		clientTeam.properties.updateFrom(team.properties);
		clientTeam.ranks.putAll(team.ranks);
		clientTeam.extraData = team.extraData == null ? null : team.extraData.copy();
		return clientTeam;
	}

	@Override
	public TeamType getType() {
		return type;
	}

	@Override
	public boolean isValid() {
		return manager.getTeam(id) != null;
	}

	public void write(FriendlyByteBuf buffer, boolean writeAllProperties) {
		buffer.writeUUID(id);

		boolean hasOwner = !ownerID.equals(Util.NIL_UUID);
		buffer.writeBoolean(hasOwner);
		if (hasOwner) {
			buffer.writeUUID(ownerID);
		}
		buffer.writeEnum(type);
		buffer.writeBoolean(toBeRemoved);

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

	}

	public UUID getOwnerID() {
		return ownerID;
	}

	public void setMessageHistory(List<TeamMessage> messages) {
		messageHistory.clear();
		messageHistory.addAll(messages);
	}

	public boolean toBeRemoved() {
		return toBeRemoved;
	}

	public void updateProperties(TeamProperties newProps) {
		TeamProperties old = properties.copy();
		properties.updateFrom(properties);

		TeamEvent.CLIENT_PROPERTIES_CHANGED.invoker().accept(new ClientTeamPropertiesChangedEvent(this, old));
	}
}
