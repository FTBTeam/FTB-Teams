package dev.ftb.mods.ftbteams.event;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamRank;

public class TeamRankChangedEvent extends TeamEvent {
    private final GameProfile player;
    private final TeamRank oldRank;
    private final TeamRank newRank;

    public TeamRankChangedEvent(Team team, GameProfile player, TeamRank oldRank, TeamRank newRank) {
        super(team);
        this.player = player;
        this.oldRank = oldRank;
        this.newRank = newRank;
    }

    public GameProfile getPlayer() {
        return player;
    }

    public TeamRank getOldRank() {
        return oldRank;
    }

    public TeamRank getNewRank() {
        return newRank;
    }
}
