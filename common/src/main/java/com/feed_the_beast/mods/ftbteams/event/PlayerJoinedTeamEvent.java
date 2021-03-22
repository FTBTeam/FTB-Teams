package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.data.FTBTUtils;
import com.feed_the_beast.mods.ftbteams.data.Team;
import com.mojang.authlib.GameProfile;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class PlayerJoinedTeamEvent extends TeamEvent {
	public static final Event<Consumer<PlayerJoinedTeamEvent>> EVENT = EventFactory.createConsumerLoop(PlayerJoinedTeamEvent.class);
	private final Optional<Team> previousTeam;
	private final GameProfile player;

	public PlayerJoinedTeamEvent(Team t, Optional<Team> t0, GameProfile p) {
		super(t);
		previousTeam = t0;
		player = p;
	}

	public Optional<Team> getPreviousTeam() {
		return previousTeam;
	}

	public GameProfile getPlayerProfile() {
		return player;
	}

	@Nullable
	public ServerPlayer getPlayer() {
		return FTBTUtils.getPlayerByProfile(getTeam().manager.server, player);
	}
}