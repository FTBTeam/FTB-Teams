package dev.ftb.mods.ftbteams;

import dev.ftb.mods.ftbteams.net.OpenCreatePartyGUIMessage;
import dev.ftb.mods.ftbteams.net.OpenMyTeamGUIMessage;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class FTBTeamsCommon {
	public void openMyTeamGui(OpenMyTeamGUIMessage res) {
	}

	public void openCreatePartyGui(OpenCreatePartyGUIMessage res) {
	}

	public void updateSettings(UUID id, TeamProperties properties) {
	}

	public void sendMessage(UUID from, Component text) {
	}
}
