package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class TeamMessageSentEvent extends TeamEvent {
    private final Component message;
    private final UUID from;

    public TeamMessageSentEvent(Team team, Component message, UUID from) {
        super(team);
        this.message = message;
        this.from = from;
    }

    public Component getMessage() {
        return message;
    }

    public UUID getFrom() {
        return from;
    }
}
