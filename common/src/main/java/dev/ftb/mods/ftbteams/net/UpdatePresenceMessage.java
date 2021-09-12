package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.data.KnownClientPlayer;
import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.networking.simple.BaseS2CMessage;
import me.shedaniel.architectury.networking.simple.MessageType;
import net.minecraft.network.FriendlyByteBuf;

public class UpdatePresenceMessage extends BaseS2CMessage {
	private final KnownClientPlayer update;

	UpdatePresenceMessage(FriendlyByteBuf buffer) {
		update = new KnownClientPlayer(buffer);
	}

	public UpdatePresenceMessage(KnownClientPlayer p) {
		update = p;
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.UPDATE_PRESENCE;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		update.write(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeams.PROXY.updatePresence(update);
	}
}
