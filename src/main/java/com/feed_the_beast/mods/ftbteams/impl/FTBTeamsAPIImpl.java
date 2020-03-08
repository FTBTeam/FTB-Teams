package com.feed_the_beast.mods.ftbteams.impl;

import com.feed_the_beast.mods.ftbteams.api.FTBTeamsAPI;
import com.feed_the_beast.mods.ftbteams.api.TeamArgument;
import com.feed_the_beast.mods.ftbteams.api.TeamManager;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class FTBTeamsAPIImpl extends FTBTeamsAPI
{
	public static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_]+$");

	@Override
	public TeamArgument argument()
	{
		return new TeamArgumentImpl(() -> FTBTeamsAPI.INSTANCE.getManager().getTeamNameMap().keySet());
	}

	@Override
	public boolean isManagerLoaded()
	{
		return TeamManagerImpl.instance != null;
	}

	@Override
	public TeamManager getManager()
	{
		return Objects.requireNonNull(TeamManagerImpl.instance);
	}

	@Override
	public boolean isValidID(String id)
	{
		if (id.isEmpty() || id.equals("ftbteams"))
		{
			return false;
		}

		return ID_PATTERN.matcher(id).matches();
	}
}