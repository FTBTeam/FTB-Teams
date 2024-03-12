package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftbteams.FTBTeamsAPIImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PlayerPermissions {
    private final boolean createParty;
    private final boolean invitePlayer;
    private final boolean addAlly;

    private PlayerPermissions(boolean createParty, boolean invitePlayer, boolean addAlly) {
        this.createParty = createParty;
        this.invitePlayer = invitePlayer;
        this.addAlly = addAlly;
    }

    public PlayerPermissions(ServerPlayer player) {
        this(
                FTBTUtils.canPlayerUseCommand(player, "ftbteams.party.create")
                        && !FTBTeamsAPIImpl.INSTANCE.isPartyCreationFromAPIOnly(),
                FTBTUtils.canPlayerUseCommand(player, "ftbteams.party.invite"),
                FTBTUtils.canPlayerUseCommand(player, "ftbteams.party.allies.add")
        );
    }

    public static PlayerPermissions fromNetwork(FriendlyByteBuf buf) {
        byte flags = buf.readByte();
        return new PlayerPermissions(
                (flags & 0x1) != 0,
                (flags & 0x2) != 0,
                (flags & 0x4) != 0
        );
    }

    public void toNetwork(FriendlyByteBuf buf) {
        byte flags = 0;
        if (createParty) flags |= 0x1;
        if (invitePlayer) flags |= 0x2;
        if (addAlly) flags |= 0x4;
        buf.writeByte(flags);
    }

    public boolean canCreateParty() {
        return createParty;
    }

    public boolean canInvitePlayer() {
        return invitePlayer;
    }

    public boolean canAddAlly() {
        return addAlly;
    }
}
