package dev.ftb.mods.ftbteams.client;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class KnownClientPlayerNet {
    public static KnownClientPlayer fromNetwork(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String name = buf.readUtf(Short.MAX_VALUE);
        boolean online = buf.readBoolean();
        UUID teamId = buf.readUUID();
        CompoundTag extraData = buf.readAnySizeNbt();

        return new KnownClientPlayer(id, name, online, teamId, new GameProfile(id, name), extraData);
    }

    public static void write(KnownClientPlayer kcp, FriendlyByteBuf buf) {
        buf.writeUUID(kcp.id());
        buf.writeUtf(kcp.name(), Short.MAX_VALUE);
        buf.writeBoolean(kcp.online());
        buf.writeUUID(kcp.teamId());
        buf.writeNbt(kcp.extraData());
    }
}
