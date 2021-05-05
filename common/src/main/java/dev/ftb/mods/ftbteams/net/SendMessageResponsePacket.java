package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftblibrary.net.BasePacket;
import dev.ftb.mods.ftblibrary.net.PacketID;
import dev.ftb.mods.ftbteams.FTBTeams;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class SendMessageResponsePacket extends BasePacket {
	private final UUID from;
	private final Component text;

	SendMessageResponsePacket(FriendlyByteBuf buffer) {
		from = buffer.readUUID();
		text = buffer.readComponent();
	}

	public SendMessageResponsePacket(UUID f, Component s) {
		from = f;
		text = s;
	}

	@Override
	public PacketID getId() {
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
