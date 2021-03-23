package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.data.FTBTUtils;
import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class PlayerChangedTeamEvent extends TeamEvent {
	public static final Event<Consumer<PlayerChangedTeamEvent>> EVENT = EventFactory.createConsumerLoop(PlayerChangedTeamEvent.class);

	private final Optional<Team> previousTeam;
	private final UUID playerId;

	public PlayerChangedTeamEvent(Team t, Optional<Team> t0, UUID p) {
		super(t);
		previousTeam = t0;
		playerId = p;
	}

	public Optional<Team> getPreviousTeam() {
		return previousTeam;
	}

	public UUID getPlayerId() {
		return playerId;
	}

	@Nullable
	public ServerPlayer getPlayer() {
		return FTBTUtils.getPlayerByUUID(getTeam().manager.server, playerId);
	}
}