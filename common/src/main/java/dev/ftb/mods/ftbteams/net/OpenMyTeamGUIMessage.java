package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import dev.ftb.mods.ftbteams.client.FTBTeamsClient;
import dev.ftb.mods.ftbteams.data.TeamPropertyCollectionImpl;
import net.minecraft.network.FriendlyByteBuf;

public class OpenMyTeamGUIMessage extends BaseS2CMessage {
	private final TeamPropertyCollection properties;

	public OpenMyTeamGUIMessage(TeamPropertyCollection properties) {
		this.properties = properties;
	}

	OpenMyTeamGUIMessage(FriendlyByteBuf buffer) {
		this.properties = TeamPropertyCollectionImpl.fromNetwork(buffer);
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.OPEN_MY_TEAM_GUI;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		properties.write(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeamsClient.openMyTeamGui(properties);
	}
}
