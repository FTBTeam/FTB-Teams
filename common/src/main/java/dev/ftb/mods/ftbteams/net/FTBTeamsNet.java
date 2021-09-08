package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeams;
import me.shedaniel.architectury.networking.simple.MessageType;
import me.shedaniel.architectury.networking.simple.SimpleNetworkManager;

public interface FTBTeamsNet {
	SimpleNetworkManager NET = SimpleNetworkManager.create(FTBTeams.MOD_ID);

	MessageType SYNC_TEAMS = NET.registerS2C("sync_teams", SyncTeamsMessage::new);
	MessageType OPEN_GUI = NET.registerC2S("open_gui", OpenGUIMessage::new);
	MessageType OPEN_MY_TEAM_GUI = NET.registerS2C("open_my_team_gui", OpenMyTeamGUIMessage::new);
	MessageType OPEN_CREATE_PARTY_GUI = NET.registerS2C("open_create_party_gui", OpenCreatePartyGUIMessage::new);
	MessageType UPDATE_SETTINGS = NET.registerC2S("update_settings", UpdateSettingsMessage::new);
	MessageType UPDATE_SETTINGS_RESPONSE = NET.registerS2C("update_settings_response", UpdateSettingsResponseMessage::new);
	MessageType SEND_MESSAGE = NET.registerC2S("send_message", SendMessageMessage::new);
	MessageType SEND_MESSAGE_RESPONSE = NET.registerS2C("send_message_response", SendMessageResponseMessage::new);

	static void init() {
	}
}
