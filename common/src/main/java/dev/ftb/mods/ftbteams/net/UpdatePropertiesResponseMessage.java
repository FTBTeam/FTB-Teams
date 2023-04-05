package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import dev.ftb.mods.ftbteams.client.FTBTeamsClient;
import dev.ftb.mods.ftbteams.data.TeamPropertyCollectionImpl;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class UpdatePropertiesResponseMessage extends BaseS2CMessage {
	private final UUID teamId;
	private final TeamPropertyCollection properties;

	UpdatePropertiesResponseMessage(FriendlyByteBuf buffer) {
		teamId = buffer.readUUID();
		properties = new TeamPropertyCollectionImpl();
		properties.read(buffer);
	}

	public UpdatePropertiesResponseMessage(UUID id, TeamPropertyCollection p) {
		teamId = id;
		properties = p;
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.UPDATE_SETTINGS_RESPONSE;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(teamId);
		properties.write(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeamsClient.updateSettings(teamId, properties);
	}
}
