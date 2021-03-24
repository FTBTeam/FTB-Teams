package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class MessageOpenGUI extends MessageBase {
	MessageOpenGUI(FriendlyByteBuf buffer) {
	}

	public MessageOpenGUI() {
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		Team team = FTBTeamsAPI.getPlayerTeam(player.getUUID());

		if (team == null) {
			return;
		}

		MessageOpenGUIResponse res = new MessageOpenGUIResponse();

		if (team instanceof PartyTeam) {
			res.messages.addAll(((PartyTeam) team).messageHistory);
		}

		res.properties.copyFrom(team.properties);
		res.sendTo(player);
	}
}
