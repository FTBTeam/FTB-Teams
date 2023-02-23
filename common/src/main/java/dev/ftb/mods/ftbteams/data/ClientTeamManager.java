package dev.ftb.mods.ftbteams.data;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbteams.FTBTeams;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the teams and known players that the client knows about; one global instance exists on the client.
 * Instances are also created server-side for when changes need to be sync'd to the client; the server side instance
 * will usually not have a complete list of client teams, but just those that needed to be added/updated/removed
 * on the client.
 */
public class ClientTeamManager {
	public static ClientTeamManager INSTANCE;  // instantiated when initial team data is sync'd on player login

	private final UUID managerId;
	private final Map<UUID, ClientTeam> teamMap;
	private final Map<UUID, KnownClientPlayer> knownPlayers;

	private boolean invalid;
	private ClientTeam selfTeam;
	private KnownClientPlayer selfKnownPlayer;

	public ClientTeamManager(UUID managerId) {
		this.managerId = managerId;
		invalid = false;
		teamMap = new HashMap<>();
		knownPlayers = new HashMap<>();
	}

	public ClientTeamManager(FriendlyByteBuf buffer) {
		this(buffer.readUUID());

		int nTeams = buffer.readVarInt();
		for (int i = 0; i < nTeams; i++) {
			ClientTeam t = ClientTeam.fromNetwork(this, buffer);
			teamMap.put(t.getId(), t);
		}

		int nPlayers = buffer.readVarInt();
		for (int i = 0; i < nPlayers; i++) {
			KnownClientPlayer knownClientPlayer = KnownClientPlayer.fromNetwork(buffer);
			knownPlayers.put(knownClientPlayer.id(), knownClientPlayer);
		}
	}

	public static ClientTeamManager createServerSide(TeamManager manager, Collection<Team> teams) {
		ClientTeamManager clientManager = new ClientTeamManager(manager.getId());

		for (Team team : teams) {
			// deleted party teams won't be in the manager's team map; use an invalid client team to indicate
			//  that the client must remove that team from its client-side manager
			ClientTeam clientTeam = manager.getTeamMap().containsKey(team.getId()) ?
					ClientTeam.copyOf(clientManager, team) :
					ClientTeam.invalidTeam(clientManager, team);
			clientManager.addTeam(clientTeam);

			if (team instanceof PlayerTeam playerTeam) {
				clientManager.knownPlayers.put(team.getId(), KnownClientPlayer.fromTeam(playerTeam));
			}
		}

		return clientManager;
	}

	public UUID getManagerId() {
		return managerId;
	}

	public boolean isInvalid() {
		return invalid;
	}

	public Collection<KnownClientPlayer> knownClientPlayers() {
		return knownPlayers.values();
	}

	public Collection<ClientTeam> getTeams() {
		return teamMap.values();
	}

	public ClientTeam selfTeam() {
		return selfTeam;
	}

	public KnownClientPlayer self() {
		return selfKnownPlayer;
	}

	public void write(FriendlyByteBuf buffer, UUID selfTeamID) {
		buffer.writeUUID(getManagerId());

		buffer.writeVarInt(teamMap.size());
		teamMap.values().forEach(clientTeam -> clientTeam.write(buffer, selfTeamID.equals(clientTeam.getId())));

		buffer.writeVarInt(knownPlayers.size());
		for (KnownClientPlayer knownClientPlayer : knownPlayers.values()) {
			knownClientPlayer.write(buffer);
		}
	}

	public void initSelfDetails(UUID selfTeamID) {
		selfTeam = teamMap.get(selfTeamID);
		UUID userId = Minecraft.getInstance().getUser().getGameProfile().getId();
		selfKnownPlayer = knownPlayers.get(userId);
		if (selfKnownPlayer == null) {
			FTBTeams.LOGGER.error("Local player id {} was not found in the known players list [{}]! FTB Teams will not be able to function correctly!",
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
			return Component.literal("System").withStyle(ChatFormatting.LIGHT_PURPLE);
		}

		KnownClientPlayer p = knownPlayers.get(id);
		return Component.literal(p == null ? "Unknown" : p.name()).withStyle(ChatFormatting.YELLOW);
	}

	public void addTeam(ClientTeam team) {
		teamMap.put(team.getId(), team);
	}

	public void invalidate() {
		teamMap.clear();
		invalid = true;
	}

	public static void syncFromServer(ClientTeamManager syncedData, UUID selfTeamID, boolean fullSync) {
		if (fullSync) {
			// complete live team manager invalidation and replacement
			syncedData.initSelfDetails(selfTeamID);
			if (INSTANCE != null) {
				INSTANCE.invalidate();
			}
			INSTANCE = syncedData;
		} else if (ClientTeamManager.INSTANCE != null) {
			// just copy the sync'd team(s) into the live client team manager
			syncedData.teamMap.forEach((teamID, clientTeam) -> {
				if (clientTeam.toBeRemoved()) {
					FTBTeams.LOGGER.debug("remove {} from client team map", teamID);
					INSTANCE.teamMap.remove(teamID);
				} else {
					ClientTeam existing = INSTANCE.teamMap.get(teamID);
					if (existing != null) {
						FTBTeams.LOGGER.debug("update {} in client team map", teamID);
					} else {
						FTBTeams.LOGGER.debug("insert {} into client team map", teamID);
					}
					INSTANCE.teamMap.put(teamID, clientTeam);
				}
			});
			INSTANCE.knownPlayers.putAll(syncedData.knownPlayers);
			INSTANCE.initSelfDetails(selfTeamID);
		}
	}

	public void updatePresence(KnownClientPlayer newPlayer) {
		KnownClientPlayer existing = ClientTeamManager.INSTANCE.knownPlayers.get(newPlayer.id());
		KnownClientPlayer toUpdate = existing == null ? newPlayer : existing.updateFrom(newPlayer);

		knownPlayers.put(toUpdate.id(), newPlayer);

		if (Platform.isDevelopmentEnvironment()) {
			FTBTeams.LOGGER.info("Updated presence of " + newPlayer.name());
		}
	}
}
