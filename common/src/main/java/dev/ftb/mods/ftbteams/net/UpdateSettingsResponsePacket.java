package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftblibrary.net.BasePacket;
import dev.ftb.mods.ftblibrary.net.PacketID;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class UpdateSettingsResponsePacket extends BasePacket {
	private final UUID teamId;
	private final TeamProperties properties;

	UpdateSettingsResponsePacket(FriendlyByteBuf buffer) {
		teamId = buffer.readUUID();
		properties = new TeamProperties();
		properties.read(buffer);
	}

	public UpdateSettingsResponsePacket(UUID id, TeamProperties p) {
		teamId = id;
		properties = p;
	}

	@Override
	public PacketID getId() {
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
