package dev.ftb.mods.ftbteams.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbteams.api.TeamMessage;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.UUID;

public record TeamMessageImpl(UUID sender, long date, Component text) implements TeamMessage {
	public static final StreamCodec<RegistryFriendlyByteBuf, TeamMessage> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, TeamMessage::sender,
			ByteBufCodecs.VAR_LONG, TeamMessage::date,
			ComponentSerialization.STREAM_CODEC, TeamMessage::text,
			TeamMessageImpl::new
	);

	public static final Codec<TeamMessage> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
			UUIDUtil.STRING_CODEC.fieldOf("from").forGetter(TeamMessage::sender),
			Codec.LONG.fieldOf("date").forGetter(TeamMessage::date),
			ComponentSerialization.CODEC.fieldOf("text").forGetter(TeamMessage::text)
	).apply(instance, TeamMessageImpl::new));

	public static final Codec<List<TeamMessage>> LIST_CODEC = ExtraCodecs.compactListCodec(CODEC);
}
