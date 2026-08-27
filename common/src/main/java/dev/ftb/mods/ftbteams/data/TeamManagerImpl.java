package dev.ftb.mods.ftbteams.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.json5.Json5Util;
import dev.ftb.mods.ftblibrary.platform.event.NativeEventPosting;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.ScoreboardTeamHelper;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.event.TeamManagerEvent;
import dev.ftb.mods.ftbteams.api.event.TeamPlayerLoggedInEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.command.TeamArgument;
import dev.ftb.mods.ftbteams.config.ServerConfig;
import dev.ftb.mods.ftbteams.net.SyncMessageHistoryMessage;
import dev.ftb.mods.ftbteams.net.SyncTeamsMessage;
import dev.ftb.mods.ftbteams.net.ToggleChatResponseMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class TeamManagerImpl implements TeamManager {
	public static final LevelResource FOLDER_NAME = new LevelResource("ftbteams");

	@Nullable
	public static TeamManagerImpl INSTANCE;

	private final MinecraftServer server;
	@Nullable
	private UUID id;
	private boolean shouldSave;
	private final Map<UUID, PlayerTeam> knownPlayers;
	private final Map<UUID, AbstractTeam> teamMap;
	private final Set<UUID> chatRedirected;
	@Nullable
	Map<String, Team> nameMap;

	public TeamManagerImpl(MinecraftServer server) {
		this.server = server;

		knownPlayers = new LinkedHashMap<>();
		teamMap = new LinkedHashMap<>();
		chatRedirected = new HashSet<>();
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
		return Optional.ofNullable(teamMap.get(teamId));
	}

	@Override
	public Optional<Team> getTeamByName(String name) {
		return Optional.ofNullable(getTeamNameMap().get(name));
	}

	@Override
	public Optional<Team> getPlayerTeamForPlayerID(UUID uuid) {
		return Optional.ofNullable(getPersonalTeamForPlayerID(uuid));
	}

	@Nullable
	public PlayerTeam getPersonalTeamForPlayerID(UUID uuid) {
		return knownPlayers.get(uuid);
	}

	@Override
	public Optional<Team> getTeamForPlayerID(UUID uuid) {
		PlayerTeam t = knownPlayers.get(uuid);
		return t == null ? Optional.empty() : Optional.of(t.getEffectiveTeam());
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

	private Path managerFile() {
		return server.getWorldPath(FOLDER_NAME).resolve("ftbteams" + Json5Util.FILE_EXT);
	}

	public void load() throws IOException {
		id = null;

		Path managerFile = managerFile();
		Path directory = managerFile.getParent();

		Files.createDirectories(managerFile.getParent());

		if (!Files.exists(managerFile)) {
			markDirty();
			saveNow();
			FTBTeams.LOGGER.info("Created initial manager file {}", managerFile);
		}
		Json5Object dataFileJson = Json5Util.load(managerFile);

		id = Json5Util.fetch(dataFileJson, "id", UUIDUtil.STRING_CODEC).orElseThrow();

		chatRedirected.clear();
		Json5Util.getJson5Array(dataFileJson, "chat_redirected").ifPresent(a -> a.forEach(el -> {
            try {
                chatRedirected.add(UUID.fromString(el.getAsString()));
            } catch (IllegalArgumentException e) {
                FTBTeams.LOGGER.error("invalid uuid {} in 'chat_redirection', ignoring", el.getAsString());
            }
        }));

		NativeEventPosting.INSTANCE.postEvent(new TeamManagerEvent.Data(this, TeamManagerEvent.Action.LOADED));

		loadAllTeams(directory);

		FTBTeams.LOGGER.info("loaded team data: {} known players, {} teams total", knownPlayers.size(), teamMap.size());
	}

	private void loadAllTeams(Path directory) {
		for (TeamType type : TeamType.values()) {
			Path dir = directory.resolve(type.getSerializedName());

			if (Files.exists(dir) && Files.isDirectory(dir)) {
				try (Stream<Path> s = Files.list(dir)) {
					s.filter(path -> path.getFileName().toString().endsWith(Json5Util.FILE_EXT)).forEach(file -> {
                        try {
                            Json5Object teamJson = Json5Util.load(file);
                            AbstractTeam team = type.createTeam(this, Json5Util.fetch(teamJson, "id", UUIDUtil.STRING_CODEC).orElseThrow());
							teamMap.put(team.id, team);
							team.deserializeJson(teamJson, server.registryAccess());
                        } catch (IOException ex) {
							FTBTeams.LOGGER.error("can't read team file {}: {}", file, ex.getMessage());
                        }
					});
				} catch (Exception ex) {
					FTBTeams.LOGGER.error("can't list directory {}: {}", dir, ex.getMessage());
				}
			}
		}

		for (AbstractTeam team : teamMap.values()) {
			if (team instanceof PlayerTeam) {
				knownPlayers.put(team.id, (PlayerTeam) team);
			}
		}

		for (AbstractTeam team : teamMap.values()) {
			if (team instanceof PartyTeam) {
				for (UUID member : team.getMembers()) {
					PlayerTeam t = knownPlayers.get(member);
					if (t != null) {
						t.setEffectiveTeam(team);
					}
				}
			}
		}
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
			NativeEventPosting.INSTANCE.postEvent(new TeamManagerEvent.Data(this, TeamManagerEvent.Action.SAVED));
			Path path = managerFile();
			try {
				Json5Util.save(path, toJson());
            } catch (IOException e) {
                FTBTeams.LOGGER.error("can't save {}: {}", path, e.getMessage());
            }
            shouldSave = false;
		}

		for (AbstractTeam team : teamMap.values()) {
			team.saveIfNeeded(directory);
		}
	}

	private void tryCreateDir(Path path) {
		try {
			Files.createDirectories(path);
		} catch (Exception ex) {
			FTBTeams.LOGGER.error("can't create directory {}: {} {}", path, ex.getClass().getName(), ex.getMessage());
		}
	}

	public Json5Object toJson() {
		Json5Object json = new Json5Object();
		Json5Util.store(json, "id", UUIDUtil.STRING_CODEC, getId());
		json.add("chat_redirected", Util.make(new Json5Array(), l ->
				chatRedirected.forEach(id -> l.add(Json5Primitive.fromString(id.toString()))))
		);
		return json;
	}

	private PartyTeam createPartyTeamInternal(UUID playerId, @Nullable ServerPlayer player, String name) {
		PartyTeam team = new PartyTeam(this, UUID.randomUUID());
		team.owner = playerId;
		teamMap.put(team.id, team);

		team.setProperty(TeamProperties.DISPLAY_NAME, name.isEmpty() ? FTBTUtils.getDefaultPartyName(server, playerId, player) : name);
		team.setProperty(TeamProperties.COLOR, FTBTUtils.randomColor());

		team.onCreated(player, playerId);
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

		FTBTeams.LOGGER.debug("player {} logged in, player team = {}", id, team);

		if (team == null) {
			FTBTeams.LOGGER.debug("creating new player team for player {}", id);

			team = createPlayerTeam(id, name);
			teamMap.put(id, team);
			knownPlayers.put(id, team);

			team.onCreated(player, id);

			syncToAll = true;
			team.onPlayerChangeTeam(null, id, player, false);

			FTBTeams.LOGGER.debug("  - team created");
		} else if (!team.getPlayerName().equals(name)) {
			FTBTeams.LOGGER.debug("updating player name: {} -> {}", team.getPlayerName(), name);
			team.setPlayerName(name);
			team.markDirty();
			markDirty();
			syncToAll = true;
		}

		FTBTeams.LOGGER.debug("syncing player team data, all = {}", syncToAll);
		if (player != null) {
			syncAllToPlayer(player, team.getEffectiveTeam());
		}
		if (syncToAll) {
			syncToAll(team.getEffectiveTeam());
		}

		FTBTeams.LOGGER.debug("updating team presence");
		team.setOnline(true);
		team.updatePresence();

		if (player != null) {
			FTBTeams.LOGGER.debug("sending team login event for {}...", player.getUUID());
			NativeEventPosting.INSTANCE.postEvent(new TeamPlayerLoggedInEvent.Data(team.getEffectiveTeam(), player));
			FTBTeams.LOGGER.debug("team login event for {} sent", player.getUUID());
		}
	}

	public void playerLoggedOut(ServerPlayer player) {
		PlayerTeam team = knownPlayers.get(player.getUUID());

		if (team != null) {
			team.setOnline(false);
			team.updatePresence();
		}
	}

	/// Sync team information about all teams to one player, along with that player's team's message history.
	/// Called on player login.
	///
	/// @param player player to sync to
	/// @param selfTeam the player's own team, which could be a party team
	public void syncAllToPlayer(ServerPlayer player, AbstractTeam selfTeam) {
		ClientTeamManagerImpl manager = ClientTeamManagerImpl.forSyncing(this, teamMap.values());

		Server2PlayNetworking.send(player, new SyncTeamsMessage(manager.setSelfTeamId(selfTeam.id), selfTeam.getTeamId(), true));
		Server2PlayNetworking.send(player, SyncMessageHistoryMessage.forTeam(selfTeam));
		Server2PlayNetworking.send(player, new ToggleChatResponseMessage(isChatRedirected(player)));
		server.getPlayerList().sendPlayerPermissionLevel(player);
	}

	/// Sync only the given team(s) to all players. Called when one or more teams are modified in any way. In practice,
	/// this will always be one or two teams (two when a player is joining or leaving a team).
	///
	/// @param teams the teams to sync, which may have been deleted already
	public void syncToAll(Team... teams) {
		if (teams.length == 0) return;

		ClientTeamManagerImpl manager = ClientTeamManagerImpl.forSyncing(this, Arrays.stream(teams).toList());

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			getTeamForPlayer(player).ifPresent(selfTeam -> {
				Server2PlayNetworking.send(player, new SyncTeamsMessage(manager.setSelfTeamId(selfTeam.getTeamId()), selfTeam.getTeamId(), false));
				if (teams.length > 1) {
					Server2PlayNetworking.send(player, SyncMessageHistoryMessage.forTeam(selfTeam));
				}
			});
		}
	}

	@Override
	public Team createPartyTeam(ServerPlayer player, String name, @Nullable String description, @Nullable Color4I color) throws CommandSyntaxException {
		return createParty(player.getUUID(), player, name, description, color);
	}

	@Override
	public Team createServerTeam(CommandSourceStack source, String name, @Nullable String description, @Nullable Color4I color, @Nullable UUID teamUUID) throws CommandSyntaxException {
		if (name.length() < 3) {
			throw TeamArgument.NAME_TOO_SHORT.create();
		}
		if (teamUUID != null && getTeamByID(teamUUID).isPresent()) {
			throw TeamArgument.TEAM_ALREADY_EXISTS.create(teamUUID.toString());
		}

		ServerPlayer player = source.getPlayer();
		UUID ownerId = player == null ? Util.NIL_UUID : player.getUUID();

		ServerTeam team = new ServerTeam(this, Objects.requireNonNullElse(teamUUID, UUID.randomUUID()));
		teamMap.put(team.id, team);

		team.setProperty(TeamProperties.DISPLAY_NAME, name);
		if (description != null) team.setProperty(TeamProperties.DESCRIPTION, description);
		if (color != null) team.setProperty(TeamProperties.COLOR, color);

		team.onCreated(player, ownerId);
		source.sendSuccess(() -> Component.translatable("ftbteams.message.created_server_team", team.getName()), true);
		syncToAll(team);

		return team;
	}

	@Override
	public void setChatRedirected(ServerPlayer player, boolean redirect) {
		if (redirect && chatRedirected.add(player.getUUID()) || !redirect && chatRedirected.remove(player.getUUID())) {
			Server2PlayNetworking.send(player, new ToggleChatResponseMessage(redirect));
			shouldSave = true;
		}
	}

	@Override
	public boolean isChatRedirected(ServerPlayer player) {
		return chatRedirected.contains(player.getUUID());
	}

	// Command Handlers //

	public PartyTeam createParty(ServerPlayer player, String name) throws CommandSyntaxException {
		return createParty(player.getUUID(), player, name, null, null);
	}

	public PartyTeam createParty(UUID playerId, @Nullable ServerPlayer player, String name, @Nullable String description, @Nullable Color4I color) throws CommandSyntaxException {
		if (player != null && !FTBTUtils.canPlayerUseCommand(player, "ftbteams.party.create")) {
			throw TeamArgument.NO_PERMISSION.create();
		}

		Team oldTeam = getTeamForPlayerID(playerId).orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(playerId));

		if (!(oldTeam instanceof PlayerTeam playerTeam)) {
			throw TeamArgument.ALREADY_IN_PARTY.create();
		}

		PartyTeam team = createPartyTeamInternal(playerId, player, name);
		if (description != null) team.setProperty(TeamProperties.DESCRIPTION, description);
		if (color != null) team.setProperty(TeamProperties.COLOR, color);

		playerTeam.setEffectiveTeam(team);

		Component playerName = player != null ? player.getName() : Component.literal(playerId.toString());
		team.addMember(playerId, TeamRank.OWNER);
		team.sendMessage(Util.NIL_UUID, Component.translatable("ftbteams.message.joined", playerName).withStyle(ChatFormatting.YELLOW));
		team.markDirty();

		ServerConfig.limitedLives().ifPresent(lives -> {
			team.setProperty(TeamProperties.LIVES_REMAINING, ServerConfig.LIMITED_LIVES.get());
			team.sendMessage(Util.NIL_UUID, Component.translatable("ftbteams.message.limited_lives", lives, lives).withStyle(ChatFormatting.GOLD));
		});

		playerTeam.removeMember(playerId);
		playerTeam.markDirty();
		playerTeam.updatePresence();
		syncToAll(team, playerTeam);
		team.onPlayerChangeTeam(playerTeam, playerId, player, false);

		if (player != null) {
			ScoreboardTeamHelper.addPlayerToTeam(server, team, player.nameAndId());
		} else {
			ScoreboardTeamHelper.addPlayerToTeam(server, team, playerId);
		}

		return team;
	}

	public Component getPlayerName(@Nullable UUID id) {
		if (id == null || id.equals(Util.NIL_UUID)) {
			return Component.literal("System").withStyle(ChatFormatting.LIGHT_PURPLE);
		}

		PlayerTeam team = knownPlayers.get(id);
		return Component.literal(team == null ? "Unknown" : team.getPlayerName()).withStyle(ChatFormatting.YELLOW);
	}

	void deleteTeam(AbstractTeam team) {
		teamMap.remove(team.getId());
		markDirty();
		saveNow();
		tryDeleteTeamFile(team.getId() + Json5Util.FILE_EXT, team.getType().getSerializedName());
	}

	private void tryDeleteTeamFile(String teamFileName, String subfolderName) {
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
