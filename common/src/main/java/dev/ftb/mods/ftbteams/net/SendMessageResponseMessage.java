package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.client.FTBTeamsClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class SendMessageResponseMessage extends BaseS2CMessage {
	private final UUID senderId;
	private final Component text;

	SendMessageResponseMessage(FriendlyByteBuf buffer) {
		senderId = buffer.readUUID();
		text = buffer.readComponent();
	}

	public SendMessageResponseMessage(UUID senderId, Component text) {
		this.senderId = senderId;
		this.text = text;
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.SEND_MESSAGE_RESPONSE;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(senderId);
		buffer.writeComponent(text);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeamsClient.sendMessage(senderId, text);
	}
}
