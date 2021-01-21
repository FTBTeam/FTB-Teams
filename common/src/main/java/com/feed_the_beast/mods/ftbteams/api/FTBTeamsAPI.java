package com.feed_the_beast.mods.ftbteams.api;

/**
 * @author LatvianModder
 */
public abstract class FTBTeamsAPI
{
	public static FTBTeamsAPI INSTANCE;

	public abstract TeamArgument argument();

	public abstract boolean isManagerLoaded();

	public abstract TeamManager getManager();

	public abstract boolean isValidID(String id);
}