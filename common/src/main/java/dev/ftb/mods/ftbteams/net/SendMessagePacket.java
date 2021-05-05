package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftblibrary.net.BasePacket;
import dev.ftb.mods.ftblibrary.net.PacketID;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class SendMessagePacket extends BasePacket {
	private final String text;

	SendMessagePacket(FriendlyByteBuf buffer) {
		text = buffer.readUtf(Short.MAX_VALUE);
	}

	public SendMessagePacket(String s) {
		text = s.length() > 5000 ? s.substring(0, 5000) : s;
	}

	@Override
	public PacketID getId() {
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
		team.sendMessage(player.getUUID(), new TextComponent(text));
	}
}
