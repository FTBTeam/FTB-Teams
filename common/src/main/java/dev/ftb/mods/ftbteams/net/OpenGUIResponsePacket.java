package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftblibrary.net.snm.BaseS2CPacket;
import dev.ftb.mods.ftblibrary.net.snm.PacketID;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

public class OpenGUIResponsePacket extends BaseS2CPacket {
	public TeamProperties properties;

	OpenGUIResponsePacket(FriendlyByteBuf buffer) {
		properties = new TeamProperties();
		properties.read(buffer);
	}

	public OpenGUIResponsePacket() {
		properties = new TeamProperties();
	}

	@Override
	public PacketID getId() {
		return FTBTeamsNet.OPEN_GUI_RESPONSE;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		properties.write(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBTeams.PROXY.openGui(this);
	}
}
