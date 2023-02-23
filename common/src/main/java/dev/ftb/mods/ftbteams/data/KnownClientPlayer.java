package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record KnownClientPlayer(UUID id, String name, boolean online, UUID teamId, GameProfile profile, CompoundTag extraData)
		implements Comparable<KnownClientPlayer> {

	public static KnownClientPlayer fromTeam(PlayerTeam playerTeam) {
		return new KnownClientPlayer(
				playerTeam.getId(),
				playerTeam.playerName,
				playerTeam.online,
				playerTeam.actualTeam.getId(),
				new GameProfile(playerTeam.getId(), playerTeam.playerName),
				playerTeam.getExtraData()
		);
	}

	public static KnownClientPlayer fromNetwork(FriendlyByteBuf buf) {
		UUID id = buf.readUUID();
		String name = buf.readUtf(Short.MAX_VALUE);
		boolean online = buf.readBoolean();
		UUID teamId = buf.readUUID();
		CompoundTag extraData = buf.readAnySizeNbt();

		return new KnownClientPlayer(id, name, online, teamId, new GameProfile(id, name), extraData);
	}

	public KnownClientPlayer updateFrom(KnownClientPlayer other) {
		return new KnownClientPlayer(id, other.name, other.online, other.teamId, other.profile, other.extraData);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUUID(id);
		buf.writeUtf(name, Short.MAX_VALUE);
		buf.writeBoolean(online);
		buf.writeUUID(teamId);
		buf.writeNbt(extraData);
	}

	public boolean isInternalTeam() {
		return teamId.equals(id);
	}

	public boolean isOnlineAndNotInParty() {
		return online && isInternalTeam();
	}

	@Override
	public int compareTo(KnownClientPlayer o) {
		int i = Boolean.compare(o.isOnlineAndNotInParty(), isOnlineAndNotInParty());
		return i == 0 ? name.compareToIgnoreCase(o.name) : i;
	}
}
