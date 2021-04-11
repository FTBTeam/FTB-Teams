package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeams;
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

		register(MessageSyncTeams.class, MessageSyncTeams::new);
		register(MessageOpenGUI.class, MessageOpenGUI::new);
		register(MessageOpenGUIResponse.class, MessageOpenGUIResponse::new);
		register(MessageUpdateSettings.class, MessageUpdateSettings::new);
		register(MessageUpdateSettingsResponse.class, MessageUpdateSettingsResponse::new);
		register(MessageSendMessage.class, MessageSendMessage::new);
		register(MessageSendMessageResponse.class, MessageSendMessageResponse::new);
	}
}
