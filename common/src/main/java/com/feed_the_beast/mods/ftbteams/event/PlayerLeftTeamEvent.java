package com.feed_the_beast.mods.ftbteams.event;

import com.feed_the_beast.mods.ftbteams.data.Team;
import com.mojang.authlib.GameProfile;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class PlayerLeftTeamEvent extends TeamEvent {
	public static final Event<Consumer<PlayerLeftTeamEvent>> EVENT = EventFactory.createConsumerLoop(PlayerLeftTeamEvent.class);
	private final GameProfile profile;

	public PlayerLeftTeamEvent(Team t, GameProfile pr) {
		super(t);
		profile = pr;
	}

	public GameProfile getProfile() {
		return profile;
	}
}