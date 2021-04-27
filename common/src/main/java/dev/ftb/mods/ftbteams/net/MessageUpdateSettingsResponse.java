package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class MessageUpdateSettingsResponse extends MessageBase {
	private final UUID teamId;
	private final TeamProperties properties;

	MessageUpdateSettingsResponse(FriendlyByteBuf buffer) {
		teamId = buffer.readUUID();
		properties = new TeamProperties();
		properties.read(buffer);
	}

	public MessageUpdateSettingsResponse(UUID id, TeamProperties p) {
		teamId = id;
		properties = p;
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
