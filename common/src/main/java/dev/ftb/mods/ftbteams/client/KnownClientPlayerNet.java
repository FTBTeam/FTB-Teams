package dev.ftb.mods.ftbteams.client;

import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class KnownClientPlayerNet {
    public static final StreamCodec<FriendlyByteBuf, KnownClientPlayer> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, KnownClientPlayer::online,
            UUIDUtil.STREAM_CODEC, KnownClientPlayer::teamId,
            ByteBufCodecs.GAME_PROFILE, KnownClientPlayer::profile,
            ByteBufCodecs.COMPOUND_TAG, KnownClientPlayer::extraData,
            KnownClientPlayer::new
    );
}
