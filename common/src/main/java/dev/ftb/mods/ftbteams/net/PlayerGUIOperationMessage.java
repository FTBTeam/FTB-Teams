package dev.ftb.mods.ftbteams.net;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PlayerGUIOperationMessage extends BaseC2SMessage {
    private final Operation op;
    private final List<UUID> targets;

    public PlayerGUIOperationMessage(Operation op, UUID target) {
        this.op = op;
        this.targets = List.of(target);
    }

    public PlayerGUIOperationMessage(Operation op, Collection<GameProfile> targets) {
        this.op = op;
        this.targets = targets.stream().map(GameProfile::getId).toList();
    }

    public PlayerGUIOperationMessage(FriendlyByteBuf buf) {
        op = buf.readEnum(Operation.class);
        targets = new ArrayList<>();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            targets.add(buf.readUUID());
        }
    }

    @Override
    public MessageType getType() {
        return FTBTeamsNet.PLAYER_GUI_OPERATION;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(op);
        buf.writeVarInt(targets.size());
        targets.forEach(buf::writeUUID);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;

        UUID senderId = context.getPlayer().getUUID();
        FTBTeamsAPI.api().getManager().getTeamForPlayerID(senderId).ifPresent(team -> {
            if (team instanceof PartyTeam partyTeam) {
                TeamRank senderRank = partyTeam.getRankForPlayer(serverPlayer.getUUID());
                targets.forEach(target -> processTarget(serverPlayer, senderRank, partyTeam, target));
            }
        });
    }

    private void processTarget(ServerPlayer sourcePlayer, TeamRank senderRank, PartyTeam partyTeam, UUID targetId) {
        if (op.requireSameTeam() && !FTBTeamsAPI.api().getManager().arePlayersInSameTeam(sourcePlayer.getUUID(), targetId)) return;

        TeamRank targetRank = partyTeam.getRankForPlayer(targetId);

        FTBTeams.LOGGER.debug("received teams operation msg {} from {} (rank {}), team {}, target {} (rank {})", op, sourcePlayer.getUUID(), senderRank, partyTeam.getName().getString(), targetId, targetRank);

        try {
            final List<GameProfile> targetProfile = List.of(new GameProfile(targetId, null));
            switch (op) {
                case KICK -> {
                    if (senderRank.getPower() > targetRank.getPower()) {
                        partyTeam.kick(sourcePlayer.createCommandSourceStack(), targetProfile);
                    }
                }
                case PROMOTE -> {
                    if (senderRank.isAtLeast(TeamRank.OWNER) && targetRank.isAtLeast(TeamRank.MEMBER)) {
                        partyTeam.promote(sourcePlayer, targetProfile);
                    }
                }
                case DEMOTE -> {
                    if (senderRank.isAtLeast(TeamRank.OWNER) && targetRank.isAtLeast(TeamRank.OFFICER)) {
                        partyTeam.demote(sourcePlayer, targetProfile);
                    }
                }
                case TRANSFER_OWNER -> {
                    if (senderRank.isAtLeast(TeamRank.OWNER)) {
                        ServerPlayer p = sourcePlayer.getServer().getPlayerList().getPlayer(targetId);
                        if (p != null) {
                            partyTeam.transferOwnership(sourcePlayer.createCommandSourceStack(), p.getGameProfile());
                        }
                    }
                }
                case LEAVE -> partyTeam.leave(sourcePlayer.getUUID());
                case INVITE -> {
                    if (senderRank.isAtLeast(TeamRank.OFFICER)) {
                        ServerPlayer p = sourcePlayer.getServer().getPlayerList().getPlayer(targetId);
                        if (p != null) {
                            // need the player to be online to receive an invitation
                            partyTeam.invite(sourcePlayer, List.of(p.getGameProfile()));
                        }
                    }
                }
                case ADD_ALLY -> {
                    if (senderRank.isAtLeast(TeamRank.OFFICER) && targetRank.isAtLeast(TeamRank.NONE)) {
                        partyTeam.addAlly(sourcePlayer.createCommandSourceStack(), targetProfile);
                    }
                }
                case REMOVE_ALLY -> {
                    if (senderRank.isAtLeast(TeamRank.OFFICER) && targetRank.isAtLeast(TeamRank.ALLY)) {
                        partyTeam.removeAlly(sourcePlayer.createCommandSourceStack(), targetProfile);
                    }
                }
            }
        } catch (CommandSyntaxException e) {
            // TODO send proper response packet?
            sourcePlayer.displayClientMessage(Component.literal(e.getMessage()).withStyle(ChatFormatting.RED), false);
        }
    }

    public enum Operation {
        PROMOTE(true),
        DEMOTE(true),
        LEAVE(true),
        KICK(true),
        TRANSFER_OWNER(true),
        INVITE(false),
        ADD_ALLY(false),
        REMOVE_ALLY(false);

        private final boolean requireSameTeam;

        Operation(boolean requireSameTeam) {
            this.requireSameTeam = requireSameTeam;
        }

        boolean requireSameTeam() {
            return requireSameTeam;
        }

        public void sendMessage(KnownClientPlayer target) {
            new PlayerGUIOperationMessage(this, target.id()).sendToServer();
        }
    }
}
