package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.client.ClientTeamManager;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import dev.ftb.mods.ftbteams.client.KnownClientPlayerNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Represents the teams and known players that the client knows about; one global instance exists on the client.
 * Instances are also created server-side for when changes need to be sync'd to the client; the server side instance
 * will usually not have a complete list of client teams, but just those that needed to be added/updated/removed
 * on the client.
 */
public class ClientTeamManagerImpl implements ClientTeamManager {
	private static final ClientTeamManagerImpl NONE = new ClientTeamManagerImpl(Util.NIL_UUID);
	private static ClientTeamManagerImpl instance = NONE;  // instantiated whenever the client receives a full team sync from server

	public static final StreamCodec<RegistryFriendlyByteBuf, ClientTeamManagerImpl> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, m -> m.managerId,
			ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ClientTeam.STREAM_CODEC), m -> m.teamMap,
			ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, KnownClientPlayerNet.STREAM_CODEC), m -> m.knownPlayers,
			ClientTeamManagerImpl::new
	);

	private final UUID managerId;
	private final Map<UUID, ClientTeam> teamMap;
	private final Map<UUID, KnownClientPlayer> knownPlayers;

	private UUID selfTeamId = Util.NIL_UUID;
	private boolean valid;
	private ClientTeam selfTeam = ClientTeam.NONE;
	private KnownClientPlayer selfKnownPlayer = KnownClientPlayer.NONE;

	public static ClientTeamManagerImpl getInstance() {
		return instance;
	}

	public static void ifPresent(Consumer<ClientTeamManagerImpl> mgr) {
		if (instance != NONE) mgr.accept(instance);
	}

	private ClientTeamManagerImpl(UUID managerId) {
		this(managerId, new HashMap<>(), new HashMap<>());
	}

	private ClientTeamManagerImpl(UUID managerId, Map<UUID,ClientTeam> teamMap, Map<UUID,KnownClientPlayer> knownPlayers) {
		this.managerId = managerId;
        this.teamMap = teamMap;
        this.knownPlayers = knownPlayers;
        valid = true;
	}

	public UUID getSelfTeamId() {
		return selfTeamId;
	}

	public ClientTeamManagerImpl setSelfTeamId(UUID selfTeamId) {
		this.selfTeamId = selfTeamId;
		return this;
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
				clientTeam.setFullSyncRequired(() -> clientManager.getSelfTeamId().equals(clientTeam.getId()));
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
		return valid && selfTeam().isValid() && selfKnownPlayer.online();
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
	public Optional<Team> getTeamByID(UUID teamId) {
		return Optional.ofNullable(teamMap.get(teamId));
	}

	@Override
	public Optional<Team> getTeamForPlayer(Player player) {
		return getKnownPlayer(player.getUUID()).flatMap(kcp -> getTeamByID(kcp.teamId()));
	}

	@Override
	public ClientTeam selfTeam() {
		return selfTeam;
	}

	@Override
	public KnownClientPlayer self() {
		return selfKnownPlayer;
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
		knownPlayers.clear();
		selfTeam = ClientTeam.NONE;
		selfKnownPlayer = KnownClientPlayer.NONE;
		valid = false;
	}

	public static void syncFromServer(ClientTeamManagerImpl syncedData, UUID selfTeamID, boolean fullSync) {
		if (fullSync) {
			// complete live team manager invalidation and replacement
			if (instance != NONE) {
				instance.invalidate();
			}
			instance = syncedData;
		} else if (instance != NONE) {
			// just copy the sync'd team(s) into the live client team manager
			syncedData.teamMap.forEach((teamID, clientTeam) -> {
				if (clientTeam.toBeRemoved()) {
					FTBTeams.LOGGER.debug("remove {} from client team map", teamID);
					instance.teamMap.remove(teamID);
				} else {
					ClientTeam existing = instance.teamMap.get(teamID);
					if (existing != null) {
						FTBTeams.LOGGER.debug("update {} in client team map", teamID);
					} else {
						FTBTeams.LOGGER.debug("insert {} into client team map", teamID);
					}
					instance.teamMap.put(teamID, clientTeam);
				}
			});
			instance.knownPlayers.putAll(syncedData.knownPlayers);
		}
		instance.initSelfDetails(selfTeamID);
	}

	private void initSelfDetails(UUID selfTeamID) {
		selfTeam = teamMap.getOrDefault(selfTeamID, ClientTeam.NONE);
		UUID userId = Minecraft.getInstance().getUser().getProfileId();
		selfKnownPlayer = knownPlayers.getOrDefault(userId, KnownClientPlayer.NONE);
		if (selfKnownPlayer == KnownClientPlayer.NONE) {
			FTBTeams.LOGGER.error("Local player id {} was not found in the known players list [{}]! FTB Teams will not be able to function correctly!",
					userId, String.join(",", knownPlayers.keySet().stream().map(UUID::toString).toList()));
		}
	}

	public void updatePresence(KnownClientPlayer newPlayer) {
		KnownClientPlayer existing = ClientTeamManagerImpl.instance.knownPlayers.getOrDefault(newPlayer.id(), KnownClientPlayer.NONE);
		KnownClientPlayer toUpdate = existing.updateFrom(newPlayer);

		knownPlayers.put(toUpdate.id(), newPlayer);
		if (toUpdate.id().equals(Minecraft.getInstance().getUser().getProfileId())) {
			selfKnownPlayer = newPlayer;
		}

        FTBTeams.LOGGER.debug("Updated presence of {}", newPlayer.name());
	}

	private KnownClientPlayer updateFrom(UUID id, KnownClientPlayer other) {
		return new KnownClientPlayer(other.online(), other.teamId(), new GameProfile(id, other.name()), other.extraData());
	}
}
