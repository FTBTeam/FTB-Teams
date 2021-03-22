package com.feed_the_beast.mods.ftbteams.client;

import com.feed_the_beast.mods.ftbteams.FTBTeamsCommon;
import com.feed_the_beast.mods.ftbteams.data.TeamMessage;

import java.util.List;

public class FTBTeamsClient extends FTBTeamsCommon {
	@Override
	public void openGui(List<TeamMessage> messages) {
		new MyTeamScreen(messages).openGui();
	}
}
