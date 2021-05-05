package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftblibrary.net.BasePacket;
import dev.ftb.mods.ftblibrary.net.PacketID;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class OpenGUIPacket extends BasePacket {
	OpenGUIPacket(FriendlyByteBuf buffer) {
	}

	public OpenGUIPacket() {
	}

	@Override
	public PacketID getId() {
		return FTBTeamsNet.OPEN_GUI;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		Team team = FTBTeamsAPI.getPlayerTeam(player);
		OpenGUIResponsePacket res = new OpenGUIResponsePacket();
		res.properties = team.properties.copy();
		res.sendTo(player);
	}
}
