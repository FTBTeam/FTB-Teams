package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import dev.ftb.mods.ftbteams.client.FTBTeamsClient;
import dev.ftb.mods.ftbteams.data.PlayerPermissions;
import dev.ftb.mods.ftbteams.data.TeamPropertyCollectionImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class OpenMyTeamGUIMessage extends BaseS2CMessage {
	private final TeamPropertyCollection properties;
	private final PlayerPermissions permissions;

	public OpenMyTeamGUIMessage(ServerPlayer player, TeamPropertyCollection properties) {
		this.properties = properties;
		this.permissions = new PlayerPermissions(player);
	}

	OpenMyTeamGUIMessage(FriendlyByteBuf buffer) {
		this.properties = TeamPropertyCollectionImpl.fromNetwork(buffer);
		this.permissions = PlayerPermissions.fromNetwork(buffer);
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.OPEN_MY_TEAM_GUI;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		properties.write(buffer);
		permissions.toNetwork(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeamsClient.openMyTeamGui(properties, permissions);
	}
}
