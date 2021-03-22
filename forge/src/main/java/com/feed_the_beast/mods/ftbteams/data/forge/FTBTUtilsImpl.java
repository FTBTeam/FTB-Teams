package com.feed_the_beast.mods.ftbteams.data.forge;

import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeHooks;

public class FTBTUtilsImpl {
	public static Component newChatWithLinks(String message) {
		return ForgeHooks.newChatWithLinks(message);
	}
}
