package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.FTBTeams;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class SendMessageResponseMessage extends BaseS2CMessage {
	private final UUID from;
	private final Component text;

	SendMessageResponseMessage(FriendlyByteBuf buffer) {
		from = buffer.readUUID();
		text = buffer.readComponent();
	}

	public SendMessageResponseMessage(UUID f, Component s) {
		from = f;
		text = s;
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.SEND_MESSAGE_RESPONSE;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(from);
		buffer.writeComponent(text);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeams.PROXY.sendMessage(from, text);
	}
}
