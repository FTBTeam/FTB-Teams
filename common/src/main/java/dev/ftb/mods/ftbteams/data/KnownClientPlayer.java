package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class KnownClientPlayer implements Comparable<KnownClientPlayer> {
	public final UUID uuid;
	public String name;
	public boolean online;
	public UUID teamId;
	private GameProfile profile;

	public KnownClientPlayer(PlayerTeam pt) {
		uuid = pt.getId();
		name = pt.playerName;
		online = pt.online;
		teamId = pt.actualTeam.getId();
	}

	public KnownClientPlayer(FriendlyByteBuf buf) {
		uuid = buf.readUUID();
		name = buf.readUtf(Short.MAX_VALUE);
		online = buf.readBoolean();
		teamId = buf.readUUID();
	}

	public void update(KnownClientPlayer p) {
		name = p.name;
		online = p.online;
		teamId = p.teamId;
		profile = null;
	}

	public GameProfile getProfile() {
		if (profile == null) {
			profile = new GameProfile(uuid, name);
		}

		return profile;
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUUID(uuid);
		buf.writeUtf(name, Short.MAX_VALUE);
		buf.writeBoolean(online);
		buf.writeUUID(teamId);
	}

	public boolean isInternalTeam() {
		return teamId.equals(uuid);
	}

	public boolean isValid() {
		return online && isInternalTeam();
	}

	@Override
	public int compareTo(KnownClientPlayer o) {
		int i = Boolean.compare(o.isValid(), isValid());
		return i == 0 ? name.compareToIgnoreCase(o.name) : i;
	}
}
