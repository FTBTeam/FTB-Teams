package com.feed_the_beast.mods.ftbteams.net;

import com.feed_the_beast.mods.ftbteams.FTBTeams;
import com.feed_the_beast.mods.ftbteams.data.TeamMessage;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MessageOpenGUIResponse extends MessageBase {
	public String displayName;
	public final List<TeamMessage> messages;

	MessageOpenGUIResponse(FriendlyByteBuf buffer) {
		long now = Instant.now().toEpochMilli();

		displayName = buffer.readUtf(Short.MAX_VALUE);

		int m = buffer.readVarInt();
		messages = new ArrayList<>(m);

		for (int i = 0; i < m; i++) {
			messages.add(new TeamMessage(now, buffer));
		}
	}

	public MessageOpenGUIResponse() {
		messages = new ArrayList<>();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		long now = Instant.now().toEpochMilli();

		buffer.writeUtf(displayName, Short.MAX_VALUE);

		buffer.writeVarInt(messages.size());

		for (TeamMessage message : messages) {
			message.write(now, buffer);
		}
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeams.PROXY.openGui(this);
	}
}
