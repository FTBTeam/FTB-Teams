package com.feed_the_beast.mods.ftbteams.impl.forge;

import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeHooks;

public class TeamImplImpl // we love this name
{
	public static Component newChatWithLinks(String message)
	{
		return ForgeHooks.newChatWithLinks(message);
	}
}
