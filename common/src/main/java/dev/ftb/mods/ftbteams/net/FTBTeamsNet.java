package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftblibrary.net.FTBNetworkHandler;
import dev.ftb.mods.ftblibrary.net.PacketID;
import dev.ftb.mods.ftbteams.FTBTeams;

public interface FTBTeamsNet {
	FTBNetworkHandler NET = FTBNetworkHandler.create(FTBTeams.MOD_ID);

	PacketID SYNC_TEAMS = NET.registerS2C("sync_teams", SyncTeamsPacket::new);
	PacketID OPEN_GUI = NET.registerC2S("open_gui", OpenGUIPacket::new);
	PacketID OPEN_GUI_RESPONSE = NET.registerS2C("open_gui_response", OpenGUIResponsePacket::new);
	PacketID UPDATE_SETTINGS = NET.registerC2S("update_settings", UpdateSettingsPacket::new);
	PacketID UPDATE_SETTINGS_RESPONSE = NET.registerS2C("update_settings_response", UpdateSettingsResponsePacket::new);
	PacketID SEND_MESSAGE = NET.registerC2S("send_message", SendMessagePacket::new);
	PacketID SEND_MESSAGE_RESPONSE = NET.registerS2C("send_message_response", SendMessageResponsePacket::new);

	static void init() {
	}
}
