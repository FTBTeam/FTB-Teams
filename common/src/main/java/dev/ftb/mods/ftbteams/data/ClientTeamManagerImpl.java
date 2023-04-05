package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.client.ClientTeamManager;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import dev.ftb.mods.ftbteams.client.KnownClientPlayerNet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents the teams and known players that the client knows about; one global instance exists on the client.
 * Instances are also created server-side for when changes need to be sync'd to the client; the server side instance
 * will usually not have a complete list of client teams, but just those that needed to be added/updated/removed
 * on the client.
 */
public class ClientTeamManagerImpl implements ClientTeamManager {
	private static ClientTeamManagerImpl INSTANCE;  // instantiated whenever the client receives a full team sync from server

	private final UUID managerId;
	private final Map<UUID, ClientTeam> teamMap;
	private final Map<UUID, KnownClientPlayer> knownPlayers;

	private boolean valid;
	private ClientTeam selfTeam;
	private KnownClientPlayer selfKnownPlayer;

	public static ClientTeamManagerImpl getInstance() {
		return INSTANCE;
	}

	private ClientTeamManagerImpl(UUID managerId) {
		this.managerId = managerId;
		valid = true;
		teamMap = new HashMap<>();
		knownPlayers = new HashMap<>();
	}

	/**
	 * Called client-side when a sync packet is received from the server. The sync may not necessarily contain
	 * all teams.
	 *
	 * @param buffer the network buffer
	 */
	public static ClientTeamManagerImpl fromNetwork(FriendlyByteBuf buffer) {
		ClientTeamManagerImpl manager = new ClientTeamManagerImpl(buffer.readUUID());

		int nTeams = buffer.readVarInt();
		for (int i = 0; i < nTeams; i++) {
			ClientTeam t = ClientTeam.fromNetwork(buffer);
			manager.teamMap.put(t.getId(), t);
		}

		int nPlayers = buffer.readVarInt();
		for (int i = 0; i < nPlayers; i++) {
			KnownClientPlayer knownClientPlayer = KnownClientPlayerNet.fromNetwork(buffer);
			manager.knownPlayers.put(knownClientPlayer.id(), knownClientPlayer);
		}

		return manager;
	}

	/**
	 * Called server-side to create a team manager for syncing one or more teams to clients.
	 *
	 * @param manager the server-side team manager
	 * @param teams the teams to be sync'd, which may not necessarily exist on the server anymore
	 * @return the client manager, ready to be sync'd
	 */
	public static ClientTeamManagerImpl forSyncing(TeamManagerImpl manager, Collection<? extends Team> teams) {
		ClientTeamManagerImpl clientManager = new ClientTeamManagerImpl(manager.getId());

		for (Team team : teams) {
			if (team instanceof AbstractTeam abstractTeam) {
				// deleted party teams won't be in the manager's team map; use an invalid client team to indicate
				//  that the client must remove that team from its client-side manager
				ClientTeam clientTeam = manager.getTeamMap().containsKey(team.getId()) ?
						ClientTeam.copyOf(abstractTeam) :
						ClientTeam.invalidTeam(abstractTeam);
				clientManager.addTeam(clientTeam);
			}

			if (team instanceof PlayerTeam playerTeam) {
				clientManager.knownPlayers.put(team.getId(), playerTeam.createClientPlayer());
			}
		}

		return clientManager;
	}

	@Override
	public UUID getManagerId() {
		return managerId;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public Collection<KnownClientPlayer> knownClientPlayers() {
		return Collections.unmodifiableCollection(knownPlayers.values());
	}

	@Override
	public Collection<Team> getTeams() {
		return Collections.unmodifiableCollection(teamMap.values());
	}

	@Override
	public ClientTeam selfTeam() {
		return selfTeam;
	}

	@Override
	public KnownClientPlayer self() {
		return selfKnownPlayer;
	}

	public void write(FriendlyByteBuf buffer, UUID selfTeamID) {
		buffer.writeUUID(getManagerId());

		buffer.writeVarInt(teamMap.size());
		teamMap.values().forEach(clientTeam -> clientTeam.write(buffer, selfTeamID.equals(clientTeam.getId())));

		buffer.writeVarInt(knownPlayers.size());
		for (KnownClientPlayer knownClientPlayer : knownPlayers.values()) {
			KnownClientPlayerNet.write(knownClientPlayer, buffer);
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

	@Override
	public Optional<KnownClientPlayer> getKnownPlayer(UUID id) {
		return Optional.ofNullable(knownPlayers.get(id));
	}

	public Optional<ClientTeam> getTeam(UUID id) {
		return Optional.ofNullable(teamMap.get(id));
	}

	@Override
	public Component formatName(@Nullable UUID id) {
		if (id == null || id.equals(Util.NIL_UUID)) {
			return Component.literal("System").withStyle(ChatFormatting.LIGHT_PURPLE);
		}

		KnownClientPlayer p = knownPlayers.get(id);
		return Component.literal(p == null ? "Unknown" : p.name()).withStyle(ChatFormatting.YELLOW);
	}

	public void addTeam(ClientTeam team) {
		teamMap.put(team.getId(), team);
	}

	private void invalidate() {
		teamMap.clear();
		valid = false;
	}

	public static void syncFromServer(ClientTeamManagerImpl syncedData, UUID selfTeamID, boolean fullSync) {
		if (fullSync) {
			// complete live team manager invalidation and replacement
			syncedData.initSelfDetails(selfTeamID);
			if (INSTANCE != null) {
				INSTANCE.invalidate();
			}
			INSTANCE = syncedData;
		} else if (ClientTeamManagerImpl.INSTANCE != null) {
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
		KnownClientPlayer existing = ClientTeamManagerImpl.INSTANCE.knownPlayers.get(newPlayer.id());
		KnownClientPlayer toUpdate = existing == null ? newPlayer : updateFrom(existing.id(), newPlayer);

		knownPlayers.put(toUpdate.id(), newPlayer);

		FTBTeams.LOGGER.debug("Updated presence of " + newPlayer.name());
	}

	private KnownClientPlayer updateFrom(UUID id, KnownClientPlayer other) {
		return new KnownClientPlayer(id, other.name(), other.online(), other.teamId(), other.profile(), other.extraData());
	}
}
