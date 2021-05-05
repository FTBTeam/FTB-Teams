package dev.ftb.mods.ftbteams.client;

import dev.ftb.mods.ftblibrary.ui.CustomClickEvent;
import dev.ftb.mods.ftblibrary.util.ClientUtils;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.FTBTeamsCommon;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.TeamMessage;
import dev.ftb.mods.ftbteams.event.ClientTeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import dev.ftb.mods.ftbteams.net.OpenGUIPacket;
import dev.ftb.mods.ftbteams.net.OpenGUIResponsePacket;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;

import java.util.UUID;

public class FTBTeamsClient extends FTBTeamsCommon {
	public static final ResourceLocation OPEN_GUI_ID = new ResourceLocation(FTBTeams.MOD_ID, "open_gui");

	public FTBTeamsClient() {
		CustomClickEvent.EVENT.register(event -> {
			if (event.getId().equals(OPEN_GUI_ID)) {
				new OpenGUIPacket().sendToServer();
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	@Override
	public void openGui(OpenGUIResponsePacket res) {
		new MyTeamScreen(res).openGui();
	}

	@Override
	public void updateSettings(UUID id, TeamProperties properties) {
		if (ClientTeamManager.INSTANCE != null) {
			ClientTeam team = ClientTeamManager.INSTANCE.getTeam(id);

			if (team != null) {
				TeamProperties old = team.properties.copy();
				team.properties.updateFrom(properties);
				TeamEvent.CLIENT_PROPERTIES_CHANGED.invoker().accept(new ClientTeamPropertiesChangedEvent(team, old));
			}
		}
	}

	@Override
	public void sendMessage(UUID from, Component text) {
		ClientTeamManager.INSTANCE.selfTeam.messageHistory.add(new TeamMessage(from, System.currentTimeMillis(), text));

		MyTeamScreen screen = ClientUtils.getCurrentGuiAs(MyTeamScreen.class);

		if (screen != null) {
			screen.chatPanel.refreshWidgets();
		}
	}
}
