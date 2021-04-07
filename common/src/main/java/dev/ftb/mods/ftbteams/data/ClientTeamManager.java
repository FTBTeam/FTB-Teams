package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientTeamManager {
	public static ClientTeamManager INSTANCE;

	public boolean invalid;
	private final UUID id;
	public final Map<UUID, ClientTeam> teamMap;
	public final Map<UUID, ClientTeam> playerTeamMap;
	public final Map<UUID, GameProfile> profileMap;
	public ClientTeam selfTeam;

	public ClientTeamManager(UUID i) {
		invalid = false;
		id = i;
		teamMap = new HashMap<>();
		playerTeamMap = new HashMap<>();
		profileMap = new HashMap<>();
	}

	public ClientTeamManager(FriendlyByteBuf buffer, long now) {
		this(buffer.readUUID());

		int ts = buffer.readVarInt();

		for (int i = 0; i < ts; i++) {
			ClientTeam t = new ClientTeam(this, buffer, now);
			teamMap.put(t.getId(), t);
		}

		int ps = buffer.readVarInt();

		for (int i = 0; i < ps; i++) {
			GameProfile profile = new GameProfile(buffer.readUUID(), buffer.readUtf(Short.MAX_VALUE));
			profileMap.put(profile.getId(), profile);
		}
	}

	public UUID getId() {
		return id;
	}

	public void write(FriendlyByteBuf buffer, long now) {
		buffer.writeUUID(getId());

		buffer.writeVarInt(teamMap.size());

		for (ClientTeam t : teamMap.values()) {
			t.write(buffer, now);
		}

		buffer.writeVarInt(profileMap.size());

		for (GameProfile profile : profileMap.values()) {
			buffer.writeUUID(profile.getId());
			buffer.writeUtf(profile.getName(), Short.MAX_VALUE);
		}
	}

	public void init(UUID self) {
		selfTeam = teamMap.get(self);

		for (ClientTeam team : teamMap.values()) {
			for (UUID member : team.getMembers()) {
				playerTeamMap.put(member, team);
			}
		}
	}

	public GameProfile getProfile(UUID id) {
		GameProfile p = profileMap.get(id);

		if (p == null) {
			return new GameProfile(id, UUIDTypeAdapter.fromUUID(id));
		}

		return p;
	}
}
