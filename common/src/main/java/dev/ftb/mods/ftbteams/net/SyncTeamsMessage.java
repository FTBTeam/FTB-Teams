package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.client.MyTeamScreen;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.networking.simple.BaseS2CMessage;
import me.shedaniel.architectury.networking.simple.MessageType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class SyncTeamsMessage extends BaseS2CMessage {
	private final ClientTeamManager manager;
	private final UUID selfTeamID;
	private final boolean fullSync;

	SyncTeamsMessage(FriendlyByteBuf buffer) {
		manager = new ClientTeamManager(buffer);
		selfTeamID = buffer.readUUID();
		fullSync = buffer.readBoolean();
	}

	public SyncTeamsMessage(ClientTeamManager manager, Team selfTeam, boolean fullSync) {
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
		ClientTeamManager.syncFromServer(manager, selfTeamID, fullSync);
		MyTeamScreen.refreshIfOpen();
	}
}
