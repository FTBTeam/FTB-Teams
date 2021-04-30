package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftblibrary.net.FTBNetworkHandler;
import dev.ftb.mods.ftblibrary.net.PacketID;
import dev.ftb.mods.ftbteams.FTBTeams;

public interface FTBTeamsNet {
	FTBNetworkHandler NET = FTBNetworkHandler.create(FTBTeams.MOD_ID);

	PacketID SYNC_TEAMS = NET.register("sync_teams", MessageSyncTeams::new);
	PacketID OPEN_GUI = NET.register("open_gui", MessageOpenGUI::new);
	PacketID OPEN_GUI_RESPONSE = NET.register("open_gui_response", MessageOpenGUIResponse::new);
	PacketID UPDATE_SETTINGS = NET.register("update_settings", MessageUpdateSettings::new);
	PacketID UPDATE_SETTINGS_RESPONSE = NET.register("update_settings_response", MessageUpdateSettingsResponse::new);
	PacketID SEND_MESSAGE = NET.register("send_message", MessageSendMessage::new);
	PacketID SEND_MESSAGE_RESPONSE = NET.register("send_message_response", MessageSendMessageResponse::new);

	static void init() {
	}
}
