package dev.ftb.mods.ftbteams.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.event.PlayerLoggedInAfterTeamEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import dev.ftb.mods.ftbteams.event.TeamManagerEvent;
import dev.ftb.mods.ftbteams.net.SyncTeamsMessage;
import dev.architectury.hooks.LevelResourceHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class TeamManager {
	public static final LevelResource FOLDER_NAME = LevelResourceHooks.create("ftbteams");
	private static final LevelResource OLD_ID_FILE = LevelResourceHooks.create("data/ftbchunks/info.json");

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
				try {
					for (Path file : Files.list(dir).filter(path -> path.getFileName().toString().endsWith(".snbt")).collect(Collectors.toList())) {
						CompoundTag nbt = SNBT.read(file);

						if (nbt != null) {
							Team team = type.factory.apply(this);
							team.id = UUID.fromString(nbt.getString("id"));
							teamMap.put(team.id, team);
							team.deserializeNBT(nbt);
						}
					}
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

		FTBTeams.LOGGER.info("Loaded FTB Teams - " + knownPlayers.size() + " known players");
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

	public ServerTeam createServerTeam(ServerPlayer player, String name) {
		ServerTeam team = new ServerTeam(this);
		team.id = UUID.randomUUID();
		teamMap.put(team.id, team);

		team.setProperty(Team.DISPLAY_NAME, name.isEmpty() ? team.id.toString().substring(0, 8) : name);
		team.setProperty(Team.COLOR, FTBTUtils.randomColor());

		team.created(player);
		return team;
	}

	public PartyTeam createPartyTeam(ServerPlayer player, String name) {
		PartyTeam team = new PartyTeam(this);
		team.id = UUID.randomUUID();
		team.owner = player.getUUID();
		teamMap.put(team.id, team);

		team.setProperty(Team.DISPLAY_NAME, name.isEmpty() ? (player.getGameProfile().getName() + "'s Party") : name);
		team.setProperty(Team.COLOR, FTBTUtils.randomColor());

		team.created(player);
		return team;
	}

	public void playerLoggedIn(@Nullable ServerPlayer player, UUID id, String name) {
		PlayerTeam team = knownPlayers.get(id);
		boolean all = false;
		boolean created = false;

		if (team == null) {
			team = new PlayerTeam(this);
			team.id = id;
			team.playerName = name;
			teamMap.put(id, team);
			knownPlayers.put(id, team);

			team.setProperty(Team.DISPLAY_NAME, team.playerName);
			team.setProperty(Team.COLOR, FTBTUtils.randomColor());

			team.ranks.put(id, TeamRank.OWNER);
			created = true;
			team.save();
			save();
			all = true;
		}

		if (!team.playerName.equals(name)) {
			team.playerName = name;
			team.save();
			save();
			all = true;
		}

		if (all) {
			syncAll();
		} else if (player != null) {
			sync(player, team.actualTeam);
		}

		if (created) {
			if (player != null) {
				team.created(player);
			} else {
				team.save();
				save();
			}

			team.changedTeam(null, id, player, false);
		}

		team.online = true;
		team.updatePresence();

		if (player != null) {
			TeamEvent.PLAYER_LOGGED_IN.invoker().accept(new PlayerLoggedInAfterTeamEvent(team.actualTeam, player));
		}
	}

	public void playerLoggedOut(ServerPlayer player) {
		PlayerTeam team = knownPlayers.get(player.getUUID());

		if (team != null) {
			team.online = false;
			team.updatePresence();
		}
	}

	public ClientTeamManager createClientTeamManager() {
		ClientTeamManager clientManager = new ClientTeamManager(getId());

		for (Team team : getTeams()) {
			ClientTeam t = new ClientTeam(clientManager, team);
			clientManager.teamMap.put(t.getId(), t);

			if (team instanceof PlayerTeam) {
				clientManager.knownPlayers.put(team.getId(), new KnownClientPlayer((PlayerTeam) team));
			}
		}

		return clientManager;
	}

	public void sync(ServerPlayer player, Team self) {
		new SyncTeamsMessage(createClientTeamManager(), self).sendTo(player);
		server.getPlayerList().sendPlayerPermissionLevel(player);
	}

	public void sync(ServerPlayer player) {
		sync(player, getPlayerTeam(player));
	}

	public void syncAll() {
		save();

		ClientTeamManager clientManager = createClientTeamManager();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			new SyncTeamsMessage(clientManager, getPlayerTeam(player)).sendTo(player);
			server.getPlayerList().sendPlayerPermissionLevel(player);
		}
	}

	// Command Handlers //

	public Pair<Integer, PartyTeam> createParty(ServerPlayer player, String name) throws CommandSyntaxException {
		UUID id = player.getUUID();
		Team oldTeam = getPlayerTeam(player);

		if (!oldTeam.getType().isPlayer()) {
			throw TeamArgument.ALREADY_IN_PARTY.create();
		}

		PartyTeam team = createPartyTeam(player, name);
		((PlayerTeam) oldTeam).actualTeam = team;

		team.ranks.put(id, TeamRank.OWNER);
		team.sendMessage(Util.NIL_UUID, new TextComponent("").append(player.getName()).append(" joined your party!").withStyle(ChatFormatting.YELLOW));
		team.save();

		oldTeam.ranks.remove(id);
		oldTeam.save();

		((PlayerTeam) oldTeam).updatePresence();
		syncAll();
		team.changedTeam(oldTeam, id, player, false);
		return Pair.of(Command.SINGLE_SUCCESS, team);
	}

	public Pair<Integer, ServerTeam> createServer(CommandSourceStack source, String name) throws CommandSyntaxException {
		ServerTeam team = createServerTeam(source.getPlayerOrException(), name);
		source.sendSuccess(new TextComponent("Created new server team ").append(team.getName()), true);
		syncAll();
		return Pair.of(Command.SINGLE_SUCCESS, team);
	}

	public Component getName(@Nullable UUID id) {
		if (id == null || id.equals(Util.NIL_UUID)) {
			return new TextComponent("System").withStyle(ChatFormatting.LIGHT_PURPLE);
		}

		PlayerTeam team = knownPlayers.get(id);
		return new TextComponent(team == null ? "Unknown" : team.playerName).withStyle(ChatFormatting.YELLOW);
	}

	public CompoundTag getExtraData() {
		return extraData;
	}
}