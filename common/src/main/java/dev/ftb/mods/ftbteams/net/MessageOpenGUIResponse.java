package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.data.TeamMessage;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MessageOpenGUIResponse extends MessageBase {
	public final List<TeamMessage> messages;
	public TeamProperties properties;

	MessageOpenGUIResponse(FriendlyByteBuf buffer) {
		long now = Instant.now().toEpochMilli();

		int m = buffer.readVarInt();
		messages = new ArrayList<>(m);

		for (int i = 0; i < m; i++) {
			messages.add(new TeamMessage(now, buffer));
		}

		properties = new TeamProperties();
		properties.read(buffer);
	}

	public MessageOpenGUIResponse() {
		messages = new ArrayList<>();
		properties = new TeamProperties();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		long now = Instant.now().toEpochMilli();

		buffer.writeVarInt(messages.size());

		for (TeamMessage message : messages) {
			message.write(now, buffer);
		}

		properties.write(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeams.PROXY.openGui(this);
	}
}
