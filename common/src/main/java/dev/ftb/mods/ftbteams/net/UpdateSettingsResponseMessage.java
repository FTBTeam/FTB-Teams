package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class UpdateSettingsResponseMessage extends BaseS2CMessage {
	private final UUID teamId;
	private final TeamProperties properties;

	UpdateSettingsResponseMessage(FriendlyByteBuf buffer) {
		teamId = buffer.readUUID();
		properties = new TeamProperties();
		properties.read(buffer);
	}

	public UpdateSettingsResponseMessage(UUID id, TeamProperties p) {
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
		FTBTeams.PROXY.updateSettings(teamId, properties);
	}
}
