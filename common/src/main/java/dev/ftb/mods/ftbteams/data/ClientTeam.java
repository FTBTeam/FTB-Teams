package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftbteams.api.TeamMessage;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.event.ClientTeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.api.property.TeamProperty;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientTeam extends AbstractTeamBase {
	private static final List<TeamProperty<?>> SYNCABLE_PROPS = List.of(TeamProperties.DISPLAY_NAME, TeamProperties.COLOR);

	private final TeamType type;
	private final UUID ownerID;
	private final boolean toBeRemoved;  // true when server is sync'ing a team deletion

	private ClientTeam(UUID id, UUID ownerId, TeamType type, boolean toBeRemoved) {
		super(id);

		this.ownerID = ownerId;
		this.type = type;
		this.toBeRemoved = toBeRemoved;
	}

	public static ClientTeam invalidTeam(AbstractTeam team) {
		return new ClientTeam(team.getId(), Util.NIL_UUID, team.getType(), true);
	}

	public static ClientTeam fromNetwork(FriendlyByteBuf buffer) {
		UUID id = buffer.readUUID();
		UUID ownerID = buffer.readBoolean() ? buffer.readUUID() : Util.NIL_UUID;
		TeamType type = buffer.readEnum(TeamType.class);
		boolean mustRemove = buffer.readBoolean();

		ClientTeam clientTeam = new ClientTeam(id, ownerID, type, mustRemove);

		clientTeam.properties.read(buffer);
		int nMembers = buffer.readVarInt();
		for (int i = 0; i < nMembers; i++) {
			clientTeam.addMember(buffer.readUUID(), buffer.readEnum(TeamRank.class));
		}

		clientTeam.extraData = buffer.readNbt();

		return clientTeam;
	}

	public static ClientTeam copyOf(AbstractTeam team) {
		ClientTeam clientTeam = new ClientTeam(team.id, team.getOwner(), team.getType(), false);
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
	public UUID getOwner() {
		return ownerID;
	}

	@Override
	public void sendMessage(UUID senderId, String message) {
		// no-op
	}

	@Override
	public List<Component> getTeamInfo() {
		return List.of();
	}

	@Override
	public boolean isClientTeam() {
		return true;
	}

	@Override
	public boolean isPlayerTeam() {
		return type == TeamType.PLAYER;
	}

	@Override
	public boolean isPartyTeam() {
		return type == TeamType.PARTY;
	}

	@Override
	public boolean isServerTeam() {
		return type == TeamType.SERVER;
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

	public void setMessageHistory(List<TeamMessage> messages) {
		messageHistory.clear();
		messageHistory.addAll(messages);
	}

	public boolean toBeRemoved() {
		return toBeRemoved;
	}

	public void updateProperties(TeamPropertyCollection newProps) {
		TeamPropertyCollection old = properties.copy();
		properties.updateFrom(newProps);

		TeamEvent.CLIENT_PROPERTIES_CHANGED.invoker().accept(new ClientTeamPropertiesChangedEvent(this, old));
	}
}
