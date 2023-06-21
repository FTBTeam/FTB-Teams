package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.data.PlayerPermissions;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class OpenMyTeamGUIMessage extends BaseS2CMessage {
	public TeamProperties properties;
	public final PlayerPermissions permissions;

	OpenMyTeamGUIMessage(FriendlyByteBuf buffer) {
		properties = new TeamProperties();
		properties.read(buffer);
		permissions = PlayerPermissions.fromNetwork(buffer);
	}

	public OpenMyTeamGUIMessage(ServerPlayer player) {
		properties = new TeamProperties();
		permissions = new PlayerPermissions(player);
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
		FTBTeams.PROXY.openMyTeamGui(this);
	}
}
