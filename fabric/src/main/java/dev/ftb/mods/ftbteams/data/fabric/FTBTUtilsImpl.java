package dev.ftb.mods.ftbteams.data.fabric;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class FTBTUtilsImpl {
	public static Component newChatWithLinks(String message) {
		return new TextComponent(message);
	}
}
