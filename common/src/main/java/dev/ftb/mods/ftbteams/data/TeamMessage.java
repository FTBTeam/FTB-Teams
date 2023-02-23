package dev.ftb.mods.ftbteams.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public record TeamMessage(UUID sender, long date, Component text) {
	public static TeamMessage fromNetwork(long now, FriendlyByteBuf buffer) {
		return new TeamMessage(buffer.readUUID(), now - buffer.readVarLong(), buffer.readComponent());
	}

	public void toNetwork(long now, FriendlyByteBuf buffer) {
		buffer.writeUUID(sender);
		buffer.writeVarLong(now - date);
		buffer.writeComponent(text);
	}
}
