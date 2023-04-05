package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbteams.api.TeamMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public record TeamMessageImpl(UUID sender, long date, Component text) implements TeamMessage {
	public static TeamMessage fromNetwork(long now, FriendlyByteBuf buffer) {
		return new TeamMessageImpl(buffer.readUUID(), now - buffer.readVarLong(), buffer.readComponent());
	}

	public static TeamMessage fromNBT(CompoundTag tag) {
		return new TeamMessageImpl(
				UUID.fromString(tag.getString("from")),
				tag.getLong("date"),
				Component.Serializer.fromJson(tag.getString("text"))
		);
	}

	public static void toNetwork(TeamMessage msg, long now, FriendlyByteBuf buffer) {
		buffer.writeUUID(msg.sender());
		buffer.writeVarLong(now - msg.date());
		buffer.writeComponent(msg.text());
	}

	public static CompoundTag toNBT(TeamMessage msg) {
		SNBTCompoundTag tag = new SNBTCompoundTag();
		tag.singleLine();
		tag.putString("from", msg.sender().toString());
		tag.putLong("date", msg.date());
		tag.putString("text", Component.Serializer.toJson(msg.text()));
		return tag;
	}
}
