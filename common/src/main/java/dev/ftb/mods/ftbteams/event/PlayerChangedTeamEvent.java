package dev.ftb.mods.ftbteams.event;

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
	private final ServerPlayer player;

	public PlayerChangedTeamEvent(Team t, @Nullable Team t0, UUID p, @Nullable ServerPlayer sp) {
		super(t);
		previousTeam = Optional.ofNullable(t0);
		playerId = p;
		player = sp;
	}

	public Optional<Team> getPreviousTeam() {
		return previousTeam;
	}

	public UUID getPlayerId() {
		return playerId;
	}

	@Nullable
	public ServerPlayer getPlayer() {
		return player;
	}
}