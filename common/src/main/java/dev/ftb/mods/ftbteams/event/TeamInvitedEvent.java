package dev.ftb.mods.ftbteams.event;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class TeamInvitedEvent extends TeamEvent {
    private final Collection<GameProfile> invited;
    private final ServerPlayer inviter;

    public TeamInvitedEvent(Team team, Collection<GameProfile> invited, ServerPlayer inviter) {
        super(team);
        this.invited = invited;
        this.inviter = inviter;
    }

    public Collection<GameProfile> getInvited() {
        return invited;
    }

    public ServerPlayer getInviter() {
        return inviter;
    }
}
