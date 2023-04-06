package dev.ftb.mods.ftbteams.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.event.PlayerLoggedInAfterTeamEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import dev.ftb.mods.ftbteams.event.TeamManagerEvent;
import dev.ftb.mods.ftbteams.net.SyncTeamsMessage;
import dev.ftb.mods.ftbteams.net.SyncMessageHistoryMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author LatvianModder
 */
public class TeamManager {
	public static final LevelResource FOLDER_NAME = new LevelResource("ftbteams");
	private static final LevelResource OLD_ID_FILE = new LevelResource("data/ftbchunks/info.json");

	public static TeamManager INSTANCE;

	public final MinecraftServer server;
	private UUID id;
	private boolean shouldSave;
	final Map<UUID, PlayerTeam> knownPlayers;
	final Map<UUID, Team> teamMap;
	Map<String, Team> nameMap;
	private CompoundTag extraData;

	public TeamManager(MinecraftServer s) {
		server = s;
		knownPlayers = new LinkedHashMap<>();
		teamMap = new LinkedHashMap<>();
		extraData = new CompoundTag();
	}

	public MinecraftServer getServer() {
		return server;
	}

	public UUID getId() {
		if (id == null) {
			id = UUID.randomUUID();
		}

		return id;
	}

	public Map<UUID, PlayerTeam> getKnownPlayers() {
		return knownPlayers;
	}

	public Map<UUID, Team> getTeamMap() {
		return teamMap;
	}

	public Collection<Team> getTeams() {
		return getTeamMap().values();
	}

	public Map<String, Team> getTeamNameMap() {
		if (nameMap == null) {
			nameMap = new HashMap<>();

			for (Team team : getTeams()) {
				nameMap.put(team.getStringID(), team);
			}
		}

		return nameMap;
	}

	@Nullable
	public Team getTeamByID(UUID uuid) {
		return uuid == Util.NIL_UUID ? null : teamMap.get(uuid);
	}

	public PlayerTeam getInternalPlayerTeam(UUID uuid) {
		return knownPlayers.get(uuid);
	}

	@Nullable
	public Team getPlayerTeam(UUID uuid) {
		PlayerTeam t = knownPlayers.get(uuid);
		return t == null ? null : t.actualTeam;
	}

	public Team getPlayerTeam(ServerPlayer player) {
		return Objects.requireNonNull(getPlayerTeam(player.getUUID()));
	}

	public boolean arePlayersInSameTeam(ServerPlayer player1, ServerPlayer player2) {
		return getPlayerTeam(player1).equals(getPlayerTeam(player2));
	}

	public UUID getPlayerTeamID(UUID id) {
		Team t = getPlayerTeam(id);
		return t == null ? id : t.getId();
	}

	public void load() {
		id = null;
		Path directory = server.getWorldPath(FOLDER_NAME);

		if (Files.notExists(directory) || !Files.isDirectory(directory)) {
			return;
		}

		CompoundTag dataFileTag = SNBT.read(directory.resolve("ftbteams.snbt"));

		if (dataFileTag != null) {
			if (dataFileTag.contains("id")) {
				id = UUID.fromString(dataFileTag.getString("id"));
			}

			extraData = dataFileTag.getCompound("extra");
			TeamManagerEvent.LOADED.invoker().accept(new TeamManagerEvent(this));
		} else {
			Path oldFile = server.getWorldPath(OLD_ID_FILE);

			if (Files.exists(oldFile)) {
				try (BufferedReader reader = Files.newBufferedReader(oldFile)) {
					id = UUID.fromString(new GsonBuilder().create().fromJson(reader, JsonObject.class).get("id").getAsString());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			save();
		}

		for (TeamType type : TeamType.MAP.values()) {
			Path dir = directory.resolve(type.getSerializedName());

			if (Files.exists(dir) && Files.isDirectory(dir)) {
				try (Stream<Path> s = Files.list(dir)) {
					s.filter(path -> path.getFileName().toString().endsWith(".snbt")).forEach(file -> {
						CompoundTag nbt = SNBT.read(file);
						if (nbt != null) {
							Team team = type.factory.apply(this);
							team.id = UUID.fromString(nbt.getString("id"));
							teamMap.put(team.id, team);
							team.deserializeNBT(nbt);
						}
					});
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		for (Team team : teamMap.values()) {
			if (team instanceof PlayerTeam) {
				knownPlayers.put(team.id, (PlayerTeam) team);
			}
		}

		for (Team team : teamMap.values()) {
			if (team instanceof PartyTeam) {
				for (UUID member : team.getMembers()) {
					PlayerTeam t = knownPlayers.get(member);
					if (t != null) {
						t.actualTeam = team;
					}
				}
			}
		}

		FTBTeams.LOGGER.info("loaded team data: {} known players, {} teams total", knownPlayers.size(), teamMap.size());
	}

	public void save() {
		shouldSave = true;
		nameMap = null;
	}

	public void saveNow() {
		Path directory = server.getWorldPath(FOLDER_NAME);

		if (Files.notExists(directory)) {
			try {
				Files.createDirectories(directory);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		if (shouldSave) {
			TeamManagerEvent.SAVED.invoker().accept(new TeamManagerEvent(this));
			SNBT.write(directory.resolve("ftbteams.snbt"), serializeNBT());
			shouldSave = false;
		}

		for (TeamType type : TeamType.MAP.values()) {
			Path path = directory.resolve(type.getSerializedName());

			if (Files.notExists(path)) {
				try {
					Files.createDirectories(path);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		for (Team team : getTeams()) {
			if (team.shouldSave) {
				SNBT.write(directory.resolve(team.getType().getSerializedName() + "/" + team.getId() + ".snbt"), team.serializeNBT());
				team.shouldSave = false;
			}
		}
	}

	public SNBTCompoundTag serializeNBT() {
		SNBTCompoundTag nbt = new SNBTCompoundTag();
		nbt.putString("id", getId().toString());
		nbt.put("extra", extraData);
		return nbt;
	}

	private ServerTeam createServerTeam(ServerPlayer player, String name) {
		ServerTeam team = new ServerTeam(this);
		team.id = UUID.randomUUID();
		teamMap.put(team.id, team);

		team.setProperty(Team.DISPLAY_NAME, name.isEmpty() ? team.id.toString().substring(0, 8) : name);
		team.setProperty(Team.COLOR, FTBTUtils.randomColor());

		team.onCreated(player);
		return team;
	}

	private PartyTeam createPartyTeam(ServerPlayer player, String name) {
		PartyTeam team = new PartyTeam(this);
		team.id = UUID.randomUUID();
		team.owner = player.getUUID();
		teamMap.put(team.id, team);

		team.setProperty(Team.DISPLAY_NAME, name.isEmpty() ? (player.getGameProfile().getName() + "'s Party") : name);
		team.setProperty(Team.COLOR, FTBTUtils.randomColor());

		team.onCreated(player);
		return team;
	}

	private PlayerTeam createPlayerTeam(@Nullable ServerPlayer player, UUID playerId, String playerName) {
		PlayerTeam team = new PlayerTeam(this);

		team.id = playerId;
		team.playerName = playerName;

		team.setProperty(Team.DISPLAY_NAME, team.playerName);
		team.setProperty(Team.COLOR, FTBTUtils.randomColor());

		team.ranks.put(playerId, TeamRank.OWNER);

		return team;
	}

	public void playerLoggedIn(@Nullable ServerPlayer player, UUID id, String name) {
		PlayerTeam team = knownPlayers.get(id);
		boolean syncToAll = false;

		FTBTeams.LOGGER.debug("player {} logged in, player team = {}", id, team);

		if (team == null) {
			FTBTeams.LOGGER.debug("creating new player team for player {}", id);

			team = createPlayerTeam(player, id, name);
			teamMap.put(id, team);
			knownPlayers.put(id, team);

			team.onCreated(player);

			syncToAll = true;
			team.changedTeam(null, id, player, false);

			FTBTeams.LOGGER.debug("  - team created");
		} else if (!team.playerName.equals(name)) {
			FTBTeams.LOGGER.debug("updating player name: {} -> {}", team.playerName, name);
			team.playerName = name;
			team.save();
			save();
			syncToAll = true;
		}

		FTBTeams.LOGGER.debug("syncing player team data, all = {}", syncToAll);
		if (player != null) {
			syncAllToPlayer(player, team.actualTeam);
		}
		if (syncToAll) {
			syncTeamsToAll(team.actualTeam);
		}

		FTBTeams.LOGGER.debug("updating team presence");
		team.online = true;
		team.updatePresence();

		if (player != null) {
			FTBTeams.LOGGER.debug("sending team login event for {}...", player.getUUID());
			TeamEvent.PLAYER_LOGGED_IN.invoker().accept(new PlayerLoggedInAfterTeamEvent(team.actualTeam, player));
			FTBTeams.LOGGER.debug("team login event for {} sent", player.getUUID());
		}
	}

	public void playerLoggedOut(ServerPlayer player) {
		PlayerTeam team = knownPlayers.get(player.getUUID());

		if (team != null) {
			team.online = false;
			team.updatePresence();
		}
	}

	private ClientTeamManager createClientTeamManager(Collection<Team> teams) {
		ClientTeamManager clientManager = new ClientTeamManager(getId());

		for (Team team : teams) {
			// deleted party teams won't be in the manager's team map; use an invalid client team to indicate
			//  that the client must remove that team from its client-side manager
			ClientTeam t = teamMap.containsKey(team.getId()) ?
					new ClientTeam(clientManager, team) :
					ClientTeam.invalidTeam(clientManager, team);
			clientManager.teamMap.put(t.getId(), t);

			if (team instanceof PlayerTeam) {
				clientManager.knownPlayers.put(team.getId(), new KnownClientPlayer((PlayerTeam) team));
			}
		}

		return clientManager;
	}

	/**
	 * Sync team information about all teams to one player, along with that player's team's message history.
	 * Called on player login.
	 *
	 * @param player player to sync to
	 * @param selfTeam the player's own team, which could be a party team
	 */
	public void syncAllToPlayer(ServerPlayer player, Team selfTeam) {
		new SyncTeamsMessage(createClientTeamManager(getTeams()), selfTeam, true).sendTo(player);
		new SyncMessageHistoryMessage(selfTeam).sendTo(player);
		server.getPlayerList().sendPlayerPermissionLevel(player);
	}

	/**
	 * Sync only the given team(s) to all players. Called when one or more teams are modified in any way. In practice,
	 * this will always be one or two teams (two when a player is joining or leaving a team).
	 *
	 * @param teams the teams to sync
	 */
	public void syncTeamsToAll(Team... teams) {
		if (teams.length == 0) return;

		ClientTeamManager manager = createClientTeamManager(Arrays.stream(teams).toList());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Team selfTeam = getPlayerTeam(player);
			new SyncTeamsMessage(manager, selfTeam, false).sendTo(player);
			if (teams.length > 1) {
				new SyncMessageHistoryMessage(selfTeam).sendTo(player);
			}
		}
	}

	// Command Handlers //

	public Pair<Integer, PartyTeam> createParty(ServerPlayer player, String name) throws CommandSyntaxException {
		return createParty(player, name, null, null);
	}

	public Pair<Integer, PartyTeam> createParty(ServerPlayer player, String name, @Nullable String description, @Nullable Color4I color) throws CommandSyntaxException {
		if (FTBTeamsAPI.partyCreationOverride != null) {
			throw TeamArgument.API_OVERRIDE.create();
		}

		UUID id = player.getUUID();
		Team oldTeam = getPlayerTeam(player);

		if (!(oldTeam instanceof PlayerTeam playerTeam)) {
			throw TeamArgument.ALREADY_IN_PARTY.create();
		}

		PartyTeam team = createPartyTeam(player, name);
		if (description != null) team.setProperty(TeamBase.DESCRIPTION, description);
		if (color != null) team.setProperty(TeamBase.COLOR, color);

		playerTeam.actualTeam = team;

		team.ranks.put(id, TeamRank.OWNER);
		team.sendMessage(Util.NIL_UUID, Component.translatable("ftbteams.message.joined", player.getName()).withStyle(ChatFormatting.YELLOW));
		team.save();

		playerTeam.ranks.remove(id);
		playerTeam.save();

		playerTeam.updatePresence();
		syncTeamsToAll(team, playerTeam);
		team.changedTeam(playerTeam, id, player, false);
		return Pair.of(Command.SINGLE_SUCCESS, team);
	}

	public Pair<Integer, ServerTeam> createServer(CommandSourceStack source, String name) throws CommandSyntaxException {
		if (name.length() < 3) {
			throw TeamArgument.NAME_TOO_SHORT.create();
		}
		ServerTeam team = createServerTeam(source.getPlayerOrException(), name);
		source.sendSuccess(Component.translatable("ftbteams.message.created_server_team", team.getName()), true);
		syncTeamsToAll(team);
		return Pair.of(Command.SINGLE_SUCCESS, team);
	}

	public Component getName(@Nullable UUID id) {
		if (id == null || id.equals(Util.NIL_UUID)) {
			return Component.literal("System").withStyle(ChatFormatting.LIGHT_PURPLE);
		}

		PlayerTeam team = knownPlayers.get(id);
		return Component.literal(team == null ? "Unknown" : team.playerName).withStyle(ChatFormatting.YELLOW);
	}

	public CompoundTag getExtraData() {
		return extraData;
	}
}
