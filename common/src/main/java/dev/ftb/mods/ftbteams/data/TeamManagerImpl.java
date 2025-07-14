package dev.ftb.mods.ftbteams.data;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.event.PlayerLoggedInAfterTeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamManagerEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.net.SyncMessageHistoryMessage;
import dev.ftb.mods.ftbteams.net.SyncTeamsMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author LatvianModder
 */
public class TeamManagerImpl implements TeamManager {
	public static final LevelResource FOLDER_NAME = new LevelResource("ftbteams");

	public static TeamManagerImpl INSTANCE;

	private final MinecraftServer server;
	private UUID id;
	private boolean shouldSave;
	private final Map<UUID, PlayerTeam> knownPlayers;
	private final Map<UUID, AbstractTeam> teamMap;
	Map<String, Team> nameMap;
	private CompoundTag extraData;

	public TeamManagerImpl(MinecraftServer s) {
		server = s;
		knownPlayers = new LinkedHashMap<>();
		teamMap = new LinkedHashMap<>();
		extraData = new CompoundTag();
	}

	@Override
	public MinecraftServer getServer() {
		return server;
	}

	@Override
	public UUID getId() {
		if (id == null) {
			id = UUID.randomUUID();
		}

		return id;
	}

	@Override
	public Map<UUID, ? extends Team> getKnownPlayerTeams() {
		return Collections.unmodifiableMap(knownPlayers);
	}

	public Map<UUID, AbstractTeam> getTeamMap() {
		return teamMap;
	}

	@Override
	public Collection<Team> getTeams() {
		return Collections.unmodifiableCollection(teamMap.values());
	}

	public Map<String, Team> getTeamNameMap() {
		if (nameMap == null) {
			nameMap = new HashMap<>();
			for (AbstractTeam team : teamMap.values()) {
				nameMap.put(team.getShortName(), team);
			}
		}

		return nameMap;
	}

	@Override
	public Optional<Team> getTeamByID(UUID teamId) {
		return Optional.of(teamMap.get(teamId));
	}

	@Override
	public Optional<Team> getTeamByName(String name) {
		return Optional.ofNullable(getTeamNameMap().get(name));
	}

	@Override
	public Optional<Team> getPlayerTeamForPlayerID(UUID uuid) {
		return Optional.ofNullable(getPersonalTeamForPlayerID(uuid));
	}

	public PlayerTeam getPersonalTeamForPlayerID(UUID uuid) {
		return knownPlayers.get(uuid);
	}

	@Override
	public Optional<Team> getTeamForPlayerID(UUID uuid) {
		PlayerTeam t = knownPlayers.get(uuid);
		return t == null ? Optional.empty() : Optional.ofNullable(t.getEffectiveTeam());
	}

	@Override
	public Optional<Team> getTeamForPlayer(ServerPlayer player) {
		return getTeamForPlayerID(player.getUUID());
	}

	@Override
	public boolean arePlayersInSameTeam(UUID id1, UUID id2) {
		return getTeamForPlayerID(id1).map(team1 -> getTeamForPlayerID(id2)
				.map(team2 -> team1.getId().equals(team2.getId())).orElse(false))
				.orElse(false);
	}

	public void load() {
		id = null;
		Path directory = server.getWorldPath(FOLDER_NAME);

		if (Files.notExists(directory) || !Files.isDirectory(directory)) {
			return;
		}

		Path ftbteamsFile = directory.resolve("ftbteams.snbt");
		if (Files.exists(ftbteamsFile)) {
			try (InputStream inputStream = Files.newInputStream(ftbteamsFile)) {
				CompoundTag dataFileTag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
				
				if (dataFileTag != null) {
					if (dataFileTag.contains("id")) {
						id = dataFileTag.read("id", UUIDUtil.CODEC).orElseThrow();
					}

					extraData = dataFileTag.getCompoundOrEmpty("extra");
					TeamManagerEvent.LOADED.invoker().accept(new TeamManagerEvent(this));
				}
			} catch (Exception ex) {
				FTBTeams.LOGGER.error("Error reading ftbteams.snbt: {}", ex.getMessage());
				FTBTeams.LOGGER.info("Creating new ftbteams.snbt file...");
				// File is corrupted, we'll create a new one on next save
			}
		} else {
			FTBTeams.LOGGER.info("ftbteams.snbt does not exist, will be created on first save");
		}

		for (TeamType type : TeamType.values()) {
			Path dir = directory.resolve(type.getSerializedName());
			FTBTeams.LOGGER.info("Looking for {} teams in directory: {}", type.getSerializedName(), dir);

			if (Files.exists(dir) && Files.isDirectory(dir)) {
				try (Stream<Path> s = Files.list(dir)) {
					s.filter(path -> path.getFileName().toString().endsWith(".snbt")).forEach(file -> {
						FTBTeams.LOGGER.info("Attempting to load team file: {}", file);
						try (InputStream inputStream = Files.newInputStream(file)) {
							CompoundTag nbt = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
							if (nbt != null) {
								FTBTeams.LOGGER.info("Successfully read team file: {}", file);
								AbstractTeam team = type.createTeam(this, nbt.read("id", UUIDUtil.CODEC).orElseThrow());
								teamMap.put(team.id, team);
								team.deserializeNBT(nbt, server.registryAccess());
								FTBTeams.LOGGER.info("Successfully loaded team: {} ({})", team.getId(), team.getShortName());
							} else {
								FTBTeams.LOGGER.warn("Team file {} returned null NBT data", file);
							}
						} catch (Exception ex) {
							FTBTeams.LOGGER.error("Error reading team file {}: {}", file, ex.getMessage());
							ex.printStackTrace();
						}
					});
				} catch (Exception ex) {
					FTBTeams.LOGGER.error("can't list directory {}: {}", dir, ex.getMessage());
				}
			} else {
				FTBTeams.LOGGER.info("Directory {} does not exist or is not a directory", dir);
			}
		}

		for (AbstractTeam team : teamMap.values()) {
			if (team instanceof PlayerTeam) {
				knownPlayers.put(team.id, (PlayerTeam) team);
				FTBTeams.LOGGER.info("Added player team to knownPlayers: {} -> {}", team.id, team.getShortName());
			}
		}

		FTBTeams.LOGGER.info("knownPlayers after loading: {}", knownPlayers.keySet());

		// First pass: Set up party team memberships
		for (AbstractTeam team : teamMap.values()) {
			if (team instanceof PartyTeam) {
				FTBTeams.LOGGER.info("Processing party team: {} with members: {}", team.getId(), team.getMembers());
				for (UUID member : team.getMembers()) {
					PlayerTeam t = knownPlayers.get(member);
					if (t != null) {
						FTBTeams.LOGGER.info("Setting effective team for player {} from {} to party {}", member, t.getEffectiveTeam().getId(), team.getId());
						t.setEffectiveTeam(team);
						FTBTeams.LOGGER.info("After setting: player {} effective team is now {}", member, t.getEffectiveTeam().getId());
					} else {
						FTBTeams.LOGGER.warn("Player team not found for member {} in party {}", member, team.getId());
					}
				}
			}
		}

		// Second pass: Resolve any saved effective team IDs
		for (PlayerTeam playerTeam : knownPlayers.values()) {
			playerTeam.resolveEffectiveTeam();
			FTBTeams.LOGGER.info("Player {} resolved effective team: {}", playerTeam.getId(), playerTeam.getEffectiveTeam().getId());
		}

		FTBTeams.LOGGER.info("loaded team data: {} known players, {} teams total", knownPlayers.size(), teamMap.size());
	}

	public void markDirty() {
		shouldSave = true;
		nameMap = null;  // in case any team has changed their stringID (i.e. "friendly" name)
	}

	public void saveNow() {
		Path directory = server.getWorldPath(FOLDER_NAME);

		if (!Files.exists(directory)) {
			tryCreateDir(directory);
			for (TeamType type : TeamType.values()) {
				tryCreateDir(directory.resolve(type.getSerializedName()));
			}
		}

		if (shouldSave) {
			TeamManagerEvent.SAVED.invoker().accept(new TeamManagerEvent(this));
			
			// FIX: Use NbtIo.writeCompressed instead of SNBT.write
			try {
				CompoundTag tag = new CompoundTag();
				SNBTCompoundTag snbtTag = serializeNBT();
				tag.put("id", snbtTag.get("id"));
				tag.put("extra", snbtTag.get("extra"));
				
				// Ensure directory exists
				Files.createDirectories(directory);
				NbtIo.writeCompressed(tag, Files.newOutputStream(directory.resolve("ftbteams.snbt")));
				FTBTeams.LOGGER.info("Successfully wrote ftbteams.snbt in GZIP format");
			} catch (IOException e) {
				FTBTeams.LOGGER.error("Error writing ftbteams.snbt: {}", e.getMessage());
			}
			
			shouldSave = false;
		}

		for (AbstractTeam team : teamMap.values()) {
			FTBTeams.LOGGER.debug("Calling saveIfNeeded for team {} (type: {})", team.getId(), team.getType().getSerializedName());
			team.saveIfNeeded(directory, server.registryAccess());
		}
	}

	private void tryCreateDir(Path path) {
		try {
			Files.createDirectories(path);
		} catch (Exception ex) {
			FTBTeams.LOGGER.error("can't create directory {}: {} {}", path, ex.getClass().getName(), ex.getMessage());
		}
	}

	public SNBTCompoundTag serializeNBT() {
		SNBTCompoundTag nbt = new SNBTCompoundTag();
		nbt.store("id", UUIDUtil.CODEC, getId());
		nbt.put("extra", extraData);
		return nbt;
	}

	private ServerTeam createServerTeam(UUID playerId, ServerPlayer player, String name) {
		ServerTeam team = new ServerTeam(this, UUID.randomUUID());
		teamMap.put(team.id, team);

		team.setProperty(TeamProperties.DISPLAY_NAME, name.isEmpty() ? team.id.toString().substring(0, 8) : name);
		team.setProperty(TeamProperties.COLOR, FTBTUtils.randomColor());

		team.onCreated(player, playerId);
		return team;
	}

	private PartyTeam createPartyTeamInternal(UUID playerId, @Nullable ServerPlayer player, String name) {
		PartyTeam team = new PartyTeam(this, UUID.randomUUID());
		team.owner = playerId;
		teamMap.put(team.id, team);

		team.setProperty(TeamProperties.DISPLAY_NAME, name.isEmpty() ? FTBTUtils.getDefaultPartyName(server, playerId, player) : name);
		team.setProperty(TeamProperties.COLOR, FTBTUtils.randomColor());

		// DON'T call onCreated() here - we'll call it after adding the member
		// team.onCreated(player, playerId);
		return team;
	}

	private PlayerTeam createPlayerTeam(UUID playerId, String playerName) {
		PlayerTeam team = new PlayerTeam(this, playerId);

		team.setPlayerName(playerName);

		team.setProperty(TeamProperties.DISPLAY_NAME, playerName);
		team.setProperty(TeamProperties.COLOR, FTBTUtils.randomColor());

		team.addMember(playerId, TeamRank.OWNER);

		return team;
	}

	public void playerLoggedIn(@Nullable ServerPlayer player, UUID id, String name) {
		PlayerTeam team = knownPlayers.get(id);
		boolean syncToAll = false;

		FTBTeams.LOGGER.info("player {} ({}) logged in, player team = {}", name, id, team);
		FTBTeams.LOGGER.info("knownPlayers contains {} players: {}", knownPlayers.size(), knownPlayers.keySet());

		if (team == null) {
			FTBTeams.LOGGER.info("creating new player team for player {} ({})", name, id);

			team = createPlayerTeam(id, name);
			teamMap.put(id, team);
			knownPlayers.put(id, team);

			team.onCreated(player, id);

			syncToAll = true;
			team.onPlayerChangeTeam(null, id, player, false);

			FTBTeams.LOGGER.info("  - team created");
		} else {
			FTBTeams.LOGGER.info("Found existing player team: {} for player {} ({})", team.getId(), name, id);
			if (!team.getPlayerName().equals(name)) {
				FTBTeams.LOGGER.info("updating player name: {} -> {}", team.getPlayerName(), name);
				team.setPlayerName(name);
				team.markDirty();
				markDirty();
				syncToAll = true;
			}
		}

		FTBTeams.LOGGER.info("syncing player team data, all = {}", syncToAll);
		if (player != null) {
			FTBTeams.LOGGER.info("effective team for player: {}", team.getEffectiveTeam().getId());
			syncAllToPlayer(player, team.getEffectiveTeam());
		}
		if (syncToAll) {
			syncToAll(team.getEffectiveTeam());
		}

		FTBTeams.LOGGER.info("updating team presence");
		team.setOnline(true);
		team.updatePresence();

		if (player != null) {
			FTBTeams.LOGGER.info("sending team login event for {}...", player.getUUID());
			TeamEvent.PLAYER_LOGGED_IN.invoker().accept(new PlayerLoggedInAfterTeamEvent(team.getEffectiveTeam(), player));
			FTBTeams.LOGGER.info("team login event for {} sent", player.getUUID());
		}
	}

	public void playerLoggedOut(ServerPlayer player) {
		PlayerTeam team = knownPlayers.get(player.getUUID());

		if (team != null) {
			team.setOnline(false);
			team.updatePresence();
		}
	}

	/**
	 * Sync team information about all teams to one player, along with that player's team's message history.
	 * Called on player login.
	 *
	 * @param player player to sync to
	 * @param selfTeam the player's own team, which could be a party team
	 */
	public void syncAllToPlayer(ServerPlayer player, AbstractTeam selfTeam) {
		ClientTeamManagerImpl manager = ClientTeamManagerImpl.forSyncing(this, teamMap.values());

		NetworkManager.sendToPlayer(player, new SyncTeamsMessage(manager.setSelfTeamId(selfTeam.id), selfTeam.getTeamId(), true));
		NetworkManager.sendToPlayer(player, SyncMessageHistoryMessage.forTeam(selfTeam));
		server.getPlayerList().sendPlayerPermissionLevel(player);
	}

	/**
	 * Sync only the given team(s) to all players. Called when one or more teams are modified in any way. In practice,
	 * this will always be one or two teams (two when a player is joining or leaving a team).
	 *
	 * @param teams the teams to sync, which may have been deleted already
	 */
	public void syncToAll(Team... teams) {
		if (teams.length == 0) return;

		ClientTeamManagerImpl manager = ClientTeamManagerImpl.forSyncing(this, Arrays.stream(teams).toList());

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			getTeamForPlayer(player).ifPresent(selfTeam -> {
				NetworkManager.sendToPlayer(player, new SyncTeamsMessage(manager.setSelfTeamId(selfTeam.getTeamId()), selfTeam.getTeamId(), false));
				if (teams.length > 1) {
					NetworkManager.sendToPlayer(player, SyncMessageHistoryMessage.forTeam(selfTeam));
				}
			});
		}
	}

	@Override
	public Team createPartyTeam(ServerPlayer player, String name, @Nullable String description, @Nullable Color4I color) throws CommandSyntaxException {
		var res = createParty(player.getUUID(), player, name, description, color);
		return res.getRight();
	}

	// Command Handlers //

	public Pair<Integer, PartyTeam> createParty(ServerPlayer player, String name) throws CommandSyntaxException {
		return createParty(player.getUUID(), player, name, null, null);
	}

	public Pair<Integer, PartyTeam> createParty(UUID playerId, @Nullable ServerPlayer player, String name, @Nullable String description, @Nullable Color4I color) throws CommandSyntaxException {
		if (player != null && !FTBTUtils.canPlayerUseCommand(player, "ftbteams.party.create")) {
			throw TeamArgument.NO_PERMISSION.create();
		}

		Team oldTeam = getTeamForPlayerID(playerId).orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(playerId));

		if (!(oldTeam instanceof PlayerTeam playerTeam)) {
			throw TeamArgument.ALREADY_IN_PARTY.create();
		}

		FTBTeams.LOGGER.info("Creating party team for player {}", playerId);
		PartyTeam team = createPartyTeamInternal(playerId, player, name);
		if (description != null) team.setProperty(TeamProperties.DESCRIPTION, description);
		if (color != null) team.setProperty(TeamProperties.COLOR, color);

		FTBTeams.LOGGER.info("Setting effective team for player {} to party {}", playerId, team.getId());
		playerTeam.setEffectiveTeam(team);

		Component playerName = player != null ? player.getName() : Component.literal(playerId.toString());
		FTBTeams.LOGGER.info("Adding member {} to party team {} with rank OWNER", playerId, team.getId());
		team.addMember(playerId, TeamRank.OWNER);
		FTBTeams.LOGGER.info("Party team {} now has {} members: {}", team.getId(), team.getMembers().size(), team.getMembers());
		
		// NOW call onCreated() after the member is added
		team.onCreated(player, playerId);
		
		team.sendMessage(Util.NIL_UUID, Component.translatable("ftbteams.message.joined", playerName).withStyle(ChatFormatting.YELLOW));
		team.markDirty();

		FTBTeams.LOGGER.info("Removing member {} from player team {}", playerId, playerTeam.getId());
		playerTeam.removeMember(playerId);
		playerTeam.markDirty();

		playerTeam.updatePresence();
		syncToAll(team, playerTeam);
		team.onPlayerChangeTeam(playerTeam, playerId, player, false);
		return Pair.of(Command.SINGLE_SUCCESS, team);
	}

	public Pair<Integer, ServerTeam> createServer(CommandSourceStack source, String name) throws CommandSyntaxException {
		if (name.length() < 3) {
			throw TeamArgument.NAME_TOO_SHORT.create();
		}
		ServerPlayer player = source.getPlayer();
		UUID playerId = player == null ? Util.NIL_UUID : player.getUUID();
		ServerTeam team = createServerTeam(playerId, source.getPlayer(), name);
		source.sendSuccess(() -> Component.translatable("ftbteams.message.created_server_team", team.getName()), true);
		syncToAll(team);
		return Pair.of(Command.SINGLE_SUCCESS, team);
	}

	public Component getPlayerName(@Nullable UUID id) {
		if (id == null || id.equals(Util.NIL_UUID)) {
			return Component.literal("System").withStyle(ChatFormatting.LIGHT_PURPLE);
		}

		PlayerTeam team = knownPlayers.get(id);
		return Component.literal(team == null ? "Unknown" : team.getPlayerName()).withStyle(ChatFormatting.YELLOW);
	}

	@Override
	public CompoundTag getExtraData() {
		return extraData;
	}

	void deleteTeam(Team team) {
		teamMap.remove(team.getId());
		markDirty();
	}

	void tryDeleteTeamFile(String teamFileName, String subfolderName) {
		Path deletedPath = getServer().getWorldPath(FOLDER_NAME).resolve("deleted");
		Path teamFilePath = getServer().getWorldPath(FOLDER_NAME).resolve(subfolderName).resolve(teamFileName);
		try {
			Files.createDirectories(deletedPath);
			Files.move(teamFilePath, deletedPath.resolve(teamFileName));
		} catch (IOException e) {
			FTBTeams.LOGGER.error("can't move {} to {}: {}", teamFileName, deletedPath, e.getMessage());
			try {
				Files.deleteIfExists(teamFilePath);
			} catch (IOException e1) {
				FTBTeams.LOGGER.error("can't delete directory {}: {}", teamFilePath, e1.getMessage());
			}
		}
	}
}