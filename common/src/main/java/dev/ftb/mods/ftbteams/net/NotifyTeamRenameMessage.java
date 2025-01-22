package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Received on: CLIENT<br>
 * Sent by server when the display name property of a team is changed to make clients aware of the new shortname
 * for command completion purposes.
 *
 * @param teamId the unique team ID
 * @param newName the new display name
 */
public record NotifyTeamRenameMessage(UUID teamId, String newName) implements CustomPacketPayload {
    public static final Type<NotifyTeamRenameMessage> TYPE = new Type<>(FTBTeamsAPI.rl("notify_team_rename"));

    public static final StreamCodec<FriendlyByteBuf, NotifyTeamRenameMessage> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, NotifyTeamRenameMessage::teamId,
            ByteBufCodecs.STRING_UTF8, NotifyTeamRenameMessage::newName,
            NotifyTeamRenameMessage::new
    );

    @Override
    public Type<NotifyTeamRenameMessage> type() {
        return TYPE;
    }

    public static void handle(NotifyTeamRenameMessage message, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ClientTeamManagerImpl.getInstance().updateDisplayName(message.teamId, message.newName);
        });
    }
}
