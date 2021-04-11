package dev.ftb.mods.ftbteams.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class TeamMessage {
	public final UUID sender;
	public final long date;
	public final Component text;

	public TeamMessage(UUID s, long d, Component c) {
		sender = s;
		date = d;
		text = c;
	}

	public TeamMessage(long now, FriendlyByteBuf buffer) {
		sender = buffer.readUUID();
		date = now - buffer.readVarLong();
		text = buffer.readComponent();
	}

	public void write(long now, FriendlyByteBuf buffer) {
		buffer.writeUUID(sender);
		buffer.writeVarLong(now - date);
		buffer.writeComponent(text);
	}
}
