package dev.ftb.mods.ftbteams.client;

import dev.architectury.event.EventResult;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftblibrary.ui.CustomClickEvent;
import dev.ftb.mods.ftblibrary.util.ClientUtils;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.FTBTeamsCommon;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.KnownClientPlayer;
import dev.ftb.mods.ftbteams.data.TeamMessage;
import dev.ftb.mods.ftbteams.event.ClientTeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import dev.ftb.mods.ftbteams.net.OpenGUIMessage;
import dev.ftb.mods.ftbteams.net.OpenMyTeamGUIMessage;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class FTBTeamsClient extends FTBTeamsCommon {
	public static final ResourceLocation OPEN_GUI_ID = new ResourceLocation(FTBTeams.MOD_ID, "open_gui");

	public FTBTeamsClient() {
		CustomClickEvent.EVENT.register(event -> {
			if (event.id().equals(OPEN_GUI_ID)) {
				new OpenGUIMessage().sendToServer();
				return EventResult.interruptTrue();
			}
			return EventResult.pass();
		});
	}

	@Override
	public void openMyTeamGui(OpenMyTeamGUIMessage res) {
		new MyTeamScreen(res).openGui();
	}

	@Override
	public void updateSettings(UUID id, TeamProperties properties) {
		if (ClientTeamManager.INSTANCE == null) {
			return;
		}

		ClientTeam team = ClientTeamManager.INSTANCE.getTeam(id);

		if (team != null) {
			TeamProperties old = team.properties.copy();
			team.properties.updateFrom(properties);
			TeamEvent.CLIENT_PROPERTIES_CHANGED.invoker().accept(new ClientTeamPropertiesChangedEvent(team, old));
		}
	}

	@Override
	public void sendMessage(UUID from, Component text) {
		if (ClientTeamManager.INSTANCE == null) {
			return;
		}

		ClientTeamManager.INSTANCE.selfTeam.messageHistory.add(new TeamMessage(from, System.currentTimeMillis(), text));

		MyTeamScreen screen = ClientUtils.getCurrentGuiAs(MyTeamScreen.class);

		if (screen != null) {
			screen.chatPanel.refreshWidgets();
		}
	}

	@Override
	public void updatePresence(KnownClientPlayer update) {
		if (ClientTeamManager.INSTANCE == null) {
			return;
		}

		KnownClientPlayer p = ClientTeamManager.INSTANCE.knownPlayers.get(update.uuid);

		if (p == null) {
			ClientTeamManager.INSTANCE.knownPlayers.put(update.uuid, update);
		} else {
			p.update(update);
		}

		if (Platform.isDevelopmentEnvironment()) {
			FTBTeams.LOGGER.info("Updated presence of " + update.name);
		}
	}
}
