package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftbteams.FTBTeamsAPIImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public record PlayerPermissions(boolean createParty, boolean invitePlayer, boolean addAlly, Component partyPreventionReason) {
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerPermissions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PlayerPermissions::createParty,
            ByteBufCodecs.BOOL, PlayerPermissions::invitePlayer,
            ByteBufCodecs.BOOL, PlayerPermissions::addAlly,
            ComponentSerialization.STREAM_CODEC, PlayerPermissions::partyPreventionReason,
            PlayerPermissions::new
    );

    public static PlayerPermissions forPlayer(ServerPlayer player) {
        var res = FTBTeamsAPIImpl.INSTANCE.validatePartyCreation(player);
        return new PlayerPermissions(
                FTBTUtils.canPlayerUseCommand(player, "ftbteams.party.create") && res.isSuccess(),
                FTBTUtils.canPlayerUseCommand(player, "ftbteams.party.invite"),
                FTBTUtils.canPlayerUseCommand(player, "ftbteams.party.allies.add"),
                res.reason()
        );
    }
}
