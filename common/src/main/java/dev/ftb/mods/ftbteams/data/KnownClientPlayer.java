package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class KnownClientPlayer implements Comparable<KnownClientPlayer> {
	public final UUID uuid;
	public String name;
	public boolean online;
	public UUID teamId;

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
	}

	public GameProfile getProfile() {
		return new GameProfile(uuid, name);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUUID(uuid);
		buf.writeUtf(name, Short.MAX_VALUE);
		buf.writeBoolean(online);
		buf.writeUUID(teamId);
	}

	@Override
	public int compareTo(KnownClientPlayer o) {
		int i = Boolean.compare(o.online, online);
		return i == 0 ? name.compareToIgnoreCase(o.name) : i;
	}
}
