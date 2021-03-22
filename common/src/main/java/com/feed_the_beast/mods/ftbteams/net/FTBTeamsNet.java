package com.feed_the_beast.mods.ftbteams.net;

import com.feed_the_beast.mods.ftbteams.FTBTeams;
import me.shedaniel.architectury.networking.NetworkChannel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class FTBTeamsNet {
	public static NetworkChannel MAIN;

	private static <T extends MessageBase> void register(Class<T> c, Function<FriendlyByteBuf, T> s) {
		MAIN.register(c, MessageBase::write, s, MessageBase::handle);
	}

	public static void init() {
		MAIN = NetworkChannel.create(new ResourceLocation(FTBTeams.MOD_ID, "main"));

		register(MessageOpenGUI.class, MessageOpenGUI::new);
	}
}
