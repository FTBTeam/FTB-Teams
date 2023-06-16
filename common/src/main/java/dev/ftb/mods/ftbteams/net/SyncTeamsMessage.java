package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.client.gui.MyTeamScreen;
import dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class SyncTeamsMessage extends BaseS2CMessage {
	private final ClientTeamManagerImpl manager;
	private final UUID selfTeamID;
	private final boolean fullSync;

	SyncTeamsMessage(FriendlyByteBuf buffer) {
		manager = ClientTeamManagerImpl.fromNetwork(buffer);
		selfTeamID = buffer.readUUID();
		fullSync = buffer.readBoolean();
	}

	public SyncTeamsMessage(ClientTeamManagerImpl manager, Team selfTeam, boolean fullSync) {
		this.manager = manager;
		this.selfTeamID = selfTeam.getId();
		this.fullSync = fullSync;
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.SYNC_TEAMS;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		manager.write(buffer, selfTeamID);
		buffer.writeUUID(selfTeamID);
		buffer.writeBoolean(fullSync);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ClientTeamManagerImpl.syncFromServer(manager, selfTeamID, fullSync);
		MyTeamScreen.refreshIfOpen();
	}
}
