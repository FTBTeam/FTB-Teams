package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public enum ToggleChatRedirectionMessage implements CustomPacketPayload {
    INSTANCE;

    public static final StreamCodec<FriendlyByteBuf, ToggleChatRedirectionMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final Type<ToggleChatRedirectionMessage> TYPE = new Type<>(FTBTeamsAPI.rl("toggle_chat_redirection"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleChatRedirectionMessage ignored, NetworkManager.PacketContext packetContext) {
        if (packetContext.getPlayer() instanceof ServerPlayer sp) {
            packetContext.queue(() -> {
                TeamManager mgr = FTBTeamsAPI.api().getManager();
                mgr.setChatRedirected(sp, !mgr.isChatRedirected(sp));
            });
        }
    }
}
