package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeams;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

public class MessageSendMessageResponse extends MessageBase {
	public final String text;

	MessageSendMessageResponse(FriendlyByteBuf buffer) {
		text = buffer.readUtf(Short.MAX_VALUE);
	}

	public MessageSendMessageResponse(String s) {
		text = s;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(text, Short.MAX_VALUE);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeams.PROXY.sendMessage(text);
	}
}
