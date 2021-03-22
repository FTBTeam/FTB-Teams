package com.feed_the_beast.mods.ftbteams.net;

import com.feed_the_beast.mods.ftbteams.FTBTeams;
import com.feed_the_beast.mods.ftbteams.data.TeamMessage;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MessageOpenGUI extends MessageBase {
	public final List<TeamMessage> messages;

	MessageOpenGUI(FriendlyByteBuf buffer) {
		long now = Instant.now().toEpochMilli();

		int m = buffer.readVarInt();
		messages = new ArrayList<>(m);

		for (int i = 0; i < m; i++) {
			messages.add(new TeamMessage(now, buffer));
		}
	}

	public MessageOpenGUI(List<TeamMessage> m) {
		messages = m;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		long now = Instant.now().toEpochMilli();

		buffer.writeVarInt(messages.size());

		for (TeamMessage message : messages) {
			message.write(now, buffer);
		}
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeams.PROXY.openGui(messages);
	}
}
