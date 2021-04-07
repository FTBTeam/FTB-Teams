package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class MessageSyncTeams extends MessageBase {
	private final ClientTeamManager manager;
	private final UUID self;

	MessageSyncTeams(FriendlyByteBuf buffer) {
		long now = System.currentTimeMillis();
		manager = new ClientTeamManager(buffer, now);
		self = buffer.readUUID();
	}

	public MessageSyncTeams(ClientTeamManager m, UUID s) {
		manager = m;
		self = s;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		long now = System.currentTimeMillis();
		manager.write(buffer, now);
		buffer.writeUUID(self);
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
