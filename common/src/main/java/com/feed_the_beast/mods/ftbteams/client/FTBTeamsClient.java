package com.feed_the_beast.mods.ftbteams.client;

import com.feed_the_beast.mods.ftbguilibrary.widget.CustomClickEvent;
import com.feed_the_beast.mods.ftbteams.FTBTeams;
import com.feed_the_beast.mods.ftbteams.FTBTeamsCommon;
import com.feed_the_beast.mods.ftbteams.net.MessageOpenGUI;
import com.feed_the_beast.mods.ftbteams.net.MessageOpenGUIResponse;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;

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
}
