package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftbteams.FTBTeams;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientTeamManager {
	public static ClientTeamManager INSTANCE;

	public boolean invalid;
	private final UUID id;
	public final Map<UUID, ClientTeam> teamMap;
	public final Map<UUID, KnownClientPlayer> knownPlayers;
	public ClientTeam selfTeam;
	public KnownClientPlayer selfKnownPlayer;

	public ClientTeamManager(UUID i) {
		invalid = false;
		id = i;
		teamMap = new HashMap<>();
		knownPlayers = new HashMap<>();
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
			KnownClientPlayer knownClientPlayer = new KnownClientPlayer(buffer);
			knownPlayers.put(knownClientPlayer.uuid, knownClientPlayer);
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

		buffer.writeVarInt(knownPlayers.size());

		for (KnownClientPlayer knownClientPlayer : knownPlayers.values()) {
			knownClientPlayer.write(buffer);
		}
	}

	public void init(UUID self, List<TeamMessage> messages) {
		selfTeam = teamMap.get(self);
		selfTeam.addMessages(messages);
		UUID userId = Minecraft.getInstance().getUser().getGameProfile().getId();
		selfKnownPlayer = knownPlayers.get(userId);
		if (selfKnownPlayer == null) {
			FTBTeams.LOGGER.warn("Local player id {} was not found in the known players list [{}]! FTB Teams will not be able to function correctly!",
					userId, String.join(",", knownPlayers.keySet().stream().map(UUID::toString).toList()));
		}
	}

	@Nullable
	public KnownClientPlayer getKnownPlayer(UUID id) {
		return knownPlayers.get(id);
	}

	@Nullable
	public ClientTeam getTeam(UUID id) {
		return teamMap.get(id);
	}

	public Component getName(@Nullable UUID id) {
		if (id == null || id.equals(Util.NIL_UUID)) {
			return new TextComponent("System").withStyle(ChatFormatting.LIGHT_PURPLE);
		}

		KnownClientPlayer p = knownPlayers.get(id);
		return new TextComponent(p == null ? "Unknown" : p.name).withStyle(ChatFormatting.YELLOW);
	}
}
