package com.feed_the_beast.mods.ftbteams;

import com.feed_the_beast.mods.ftbteams.data.TeamArgument;
import com.feed_the_beast.mods.ftbteams.data.TeamManager;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public class FTBTeamsAPI {
	public static TeamArgument argument() {
		return new TeamArgument(() -> getManager().getTeamNameMap().keySet());
	}

	public static boolean isManagerLoaded() {
		return TeamManager.INSTANCE != null;
	}

	public static TeamManager getManager() {
		return Objects.requireNonNull(TeamManager.INSTANCE);
	}
}