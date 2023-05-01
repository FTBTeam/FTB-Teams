package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.server.level.ServerPlayer;

public class TeamInviteDenied extends TeamEvent {
    private final ServerPlayer player;

    public TeamInviteDenied(Team team, ServerPlayer player) {
        super(team);
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
