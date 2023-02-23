package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class UpdateSettingsMessage extends BaseC2SMessage {
	private final TeamProperties properties;

	UpdateSettingsMessage(FriendlyByteBuf buffer) {
		this.properties = TeamProperties.fromNetwork(buffer);
	}

	public UpdateSettingsMessage(TeamProperties properties) {
		this.properties = properties;
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.UPDATE_SETTINGS;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		properties.write(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		Team team = FTBTeamsAPI.getPlayerTeam(player);

		if (!team.isOfficer(player.getUUID())) {
			return;
		}

		team.updatePropertiesFrom(properties);

		new UpdateSettingsResponseMessage(team.getId(), team.getProperties()).sendToAll(player.server);
	}
}
