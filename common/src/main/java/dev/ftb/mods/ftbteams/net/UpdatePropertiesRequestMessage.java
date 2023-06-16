package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import dev.ftb.mods.ftbteams.data.AbstractTeam;
import dev.ftb.mods.ftbteams.data.TeamPropertyCollectionImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class UpdatePropertiesRequestMessage extends BaseC2SMessage {
	private final TeamPropertyCollection properties;

	public UpdatePropertiesRequestMessage(TeamPropertyCollection properties) {
		this.properties = properties;
	}

	UpdatePropertiesRequestMessage(FriendlyByteBuf buffer) {
		this.properties = TeamPropertyCollectionImpl.fromNetwork(buffer);
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

		FTBTeamsAPI.api().getManager().getTeamForPlayer(player).ifPresent(team -> {
			if (team instanceof AbstractTeam abstractTeam && abstractTeam.isOfficerOrBetter(player.getUUID())) {
				abstractTeam.updatePropertiesFrom(properties);
				new UpdatePropertiesResponseMessage(team.getId(), abstractTeam.getProperties()).sendToAll(player.server);
			}
		});
	}
}
