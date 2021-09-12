package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeams;
import me.shedaniel.architectury.networking.simple.MessageType;
import me.shedaniel.architectury.networking.simple.SimpleNetworkManager;

public interface FTBTeamsNet {
	SimpleNetworkManager NET = SimpleNetworkManager.create(FTBTeams.MOD_ID);

	MessageType SYNC_TEAMS = NET.registerS2C("sync_teams", SyncTeamsMessage::new);
	MessageType OPEN_GUI = NET.registerC2S("open_gui", OpenGUIMessage::new);
	MessageType OPEN_MY_TEAM_GUI = NET.registerS2C("open_my_team_gui", OpenMyTeamGUIMessage::new);
	MessageType UPDATE_SETTINGS = NET.registerC2S("update_settings", UpdateSettingsMessage::new);
	MessageType UPDATE_SETTINGS_RESPONSE = NET.registerS2C("update_settings_response", UpdateSettingsResponseMessage::new);
	MessageType SEND_MESSAGE = NET.registerC2S("send_message", SendMessageMessage::new);
	MessageType SEND_MESSAGE_RESPONSE = NET.registerS2C("send_message_response", SendMessageResponseMessage::new);
	MessageType UPDATE_PRESENCE = NET.registerS2C("update_presence", UpdatePresenceMessage::new);

	static void init() {
	}
}
