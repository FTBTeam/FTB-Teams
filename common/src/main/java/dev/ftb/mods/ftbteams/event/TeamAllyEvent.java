package dev.ftb.mods.ftbteams.event;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.data.Team;

import java.util.Collection;
import java.util.List;

public class TeamAllyEvent extends TeamEvent {
    private final List<GameProfile> players;
    private final boolean adding;

    public TeamAllyEvent(Team t, List<GameProfile> players, boolean adding) {
        super(t);
        this.players = players;
        this.adding = adding;
    }

    public Collection<GameProfile> getPlayers() {
        return players;
    }

    public boolean isAdding() {
        return adding;
    }
}
