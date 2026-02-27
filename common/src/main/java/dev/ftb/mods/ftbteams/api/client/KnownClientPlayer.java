package dev.ftb.mods.ftbteams.api.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents the client's knowledge of some player on the server, and their team relationships.
 *
 * @param online is the player currently online?
 * @param teamId the player's team ID (same as their UUID if they are not in a party)
 * @param extraData any extra data relating to the player
 */
public record KnownClientPlayer(boolean online, UUID teamId, GameProfile profile, @Nullable CompoundTag extraData) {
	public static final KnownClientPlayer NONE = new KnownClientPlayer(
			false,
			Util.NIL_UUID,
			new GameProfile(Util.NIL_UUID, "???"),
			null
	);

	public KnownClientPlayer updateFrom(KnownClientPlayer other) {
		return online ?
				new KnownClientPlayer(other.online(), other.teamId(), new GameProfile(this.id(), other.name()), other.extraData()) :
				other;
	}

	public UUID id() {
		return profile.id();
	}

	public String name() {
		return profile.name();
	}

	/**
	 * Is the player in their own team (i.e. not in a party)?
	 *
	 * @return true if the player is in their own personal team right now
	 */
	public boolean isInternalTeam() {
		return teamId.equals(id());
	}

	/**
	 * Check if the player is online and not in a party.
	 *
	 * @return true if the player is online and not in a party
	 */
	public boolean isOnlineAndNotInParty() {
		return online && isInternalTeam();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KnownClientPlayer that = (KnownClientPlayer) o;
		return id().equals(that.id());
	}

	@Override
	public int hashCode() {
		return Objects.hash(id());
	}
}
