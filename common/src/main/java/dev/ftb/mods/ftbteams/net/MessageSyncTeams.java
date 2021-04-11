package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamMessage;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessageSyncTeams extends MessageBase {
	private final ClientTeamManager manager;
	private final UUID self;
	private final List<TeamMessage> messages;

	MessageSyncTeams(FriendlyByteBuf buffer) {
		long now = System.currentTimeMillis();
		manager = new ClientTeamManager(buffer, now);
		self = buffer.readUUID();
		int ms = buffer.readVarInt();
		messages = new ArrayList<>(ms);

		for (int i = 0; i < ms; i++) {
			messages.add(new TeamMessage(now, buffer));
		}
	}

	public MessageSyncTeams(ClientTeamManager m, Team s) {
		manager = m;
		self = s.getId();
		messages = new ArrayList<>(s.messageHistory);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		long now = System.currentTimeMillis();
		manager.write(buffer, now);
		buffer.writeUUID(self);
		buffer.writeVarInt(messages.size());

		for (TeamMessage tm : messages) {
			tm.write(now, buffer);
		}
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		manager.init(self);

		if (ClientTeamManager.INSTANCE != null) {
			ClientTeamManager.INSTANCE.invalid = true;
		}

		ClientTeamManager.INSTANCE = manager;
	}
}
