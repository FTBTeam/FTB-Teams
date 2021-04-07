package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class TeamMessage {
	public final GameProfile sender;
	public final long date;
	public final Component text;

	public TeamMessage(GameProfile s, long d, Component c) {
		sender = s;
		date = d;
		text = c;
	}

	public TeamMessage(long now, FriendlyByteBuf buffer) {
		sender = new GameProfile(buffer.readUUID(), buffer.readUtf(Short.MAX_VALUE));
		date = now - buffer.readVarLong();
		text = buffer.readComponent();
	}

	public void write(long now, FriendlyByteBuf buffer) {
		buffer.writeUUID(sender.getId());
		buffer.writeUtf(sender.getName(), Short.MAX_VALUE);
		buffer.writeVarLong(now - date);
		buffer.writeComponent(text);
	}
}
