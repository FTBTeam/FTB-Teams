package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.AbstractTeam;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class OpenGUIMessage extends BaseC2SMessage {
	OpenGUIMessage(FriendlyByteBuf buffer) {
	}

	public OpenGUIMessage() {
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.OPEN_GUI;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		FTBTeamsAPI.api().getManager().getTeamForPlayer(player).ifPresent(team -> {
			if (team instanceof AbstractTeam abstractTeam) {
				new OpenMyTeamGUIMessage(abstractTeam.getProperties()).sendTo(player);
			}
		});
	}
}
