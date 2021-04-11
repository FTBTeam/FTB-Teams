package dev.ftb.mods.ftbteams.client;

import dev.ftb.mods.ftbguilibrary.utils.ClientUtils;
import dev.ftb.mods.ftbguilibrary.widget.CustomClickEvent;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.FTBTeamsCommon;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.TeamMessage;
import dev.ftb.mods.ftbteams.net.MessageOpenGUI;
import dev.ftb.mods.ftbteams.net.MessageOpenGUIResponse;
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
				new MessageOpenGUI().sendToServer();
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	@Override
	public void openGui(MessageOpenGUIResponse res) {
		new MyTeamScreen(res).openGui();
	}

	@Override
	public void updateSettings(TeamProperties properties) {
		if (ClientTeamManager.INSTANCE != null) {
			ClientTeamManager.INSTANCE.selfTeam.properties.updateFrom(properties);
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
