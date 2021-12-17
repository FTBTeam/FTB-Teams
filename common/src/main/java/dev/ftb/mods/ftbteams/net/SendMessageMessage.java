package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftblibrary.util.TextComponentUtils;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class SendMessageMessage extends BaseC2SMessage {
	private final String text;

	SendMessageMessage(FriendlyByteBuf buffer) {
		text = buffer.readUtf(Short.MAX_VALUE);
	}

	public SendMessageMessage(String s) {
		text = s.length() > 5000 ? s.substring(0, 5000) : s;
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.SEND_MESSAGE;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(text, Short.MAX_VALUE);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		Team team = FTBTeamsAPI.getPlayerTeam(player);
		team.sendMessage(player.getUUID(), TextComponentUtils.withLinks(text));
	}
}
