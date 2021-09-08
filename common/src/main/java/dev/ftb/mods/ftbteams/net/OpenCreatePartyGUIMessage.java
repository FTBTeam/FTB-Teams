package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.networking.simple.BaseS2CMessage;
import me.shedaniel.architectury.networking.simple.MessageType;
import net.minecraft.network.FriendlyByteBuf;

public class OpenCreatePartyGUIMessage extends BaseS2CMessage {
	public TeamProperties properties;

	OpenCreatePartyGUIMessage(FriendlyByteBuf buffer) {
		properties = new TeamProperties();
		properties.read(buffer);
	}

	public OpenCreatePartyGUIMessage() {
		properties = new TeamProperties();
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.OPEN_CREATE_PARTY_GUI;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		properties.write(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeams.PROXY.openCreatePartyGui(this);
	}
}
