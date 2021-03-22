package com.feed_the_beast.mods.ftbteams.net;

import com.feed_the_beast.mods.ftbteams.FTBTeamsAPI;
import com.feed_the_beast.mods.ftbteams.data.PartyTeam;
import com.feed_the_beast.mods.ftbteams.data.Team;
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
		res.displayName = team.getDisplayName();

		if (team instanceof PartyTeam) {
			res.messages.addAll(((PartyTeam) team).messageHistory);
		}

		res.sendTo(player);
	}
}
