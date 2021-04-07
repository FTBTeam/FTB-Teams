package dev.ftb.mods.ftbteams.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.event.TeamDeletedEvent;
import dev.ftb.mods.ftbteams.net.MessageSyncTeams;
import me.shedaniel.architectury.hooks.LevelResourceHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
	final Map<UUID, Team> playerTeamMap;
	Map<String, Team> nameMap;

	public TeamManager(MinecraftServer s) {
		server = s;
		knownPlayers = new LinkedHashMap<>();
		teamMap = new LinkedHashMap<>();
		playerTeamMap = new LinkedHashMap<>();
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
		return playerTeamMap.get(uuid);
	}

	public Team getPlayerTeam(ServerPlayer player) {
		return Objects.requireNonNull(getPlayerTeam(player.getUUID()));
	}

	public boolean arePlayersInSameTeam(ServerPlayer player1, ServerPlayer player2) {
		return getPlayerTeam(player1).equals(getPlayerTeam(player2));
	}

	public UUID getPlayerTeamID(UUID profile) {
		Team team = playerTeamMap.get(profile);
		return team == null ? profile : team.getId();
	}

	public void load() {
		id = null;
		Path directory = server.getWorldPath(FOLDER_NAME);

		if (Files.notExists(directory) || !Files.isDirectory(directory)) {
			return;
		}

		Path dataFile = directory.resolve("ftbteams.nbt");

		if (Files.exists(dataFile)) {
			try (InputStream stream = Files.newInputStream(dataFile)) {
				CompoundTag tag = Objects.requireNonNull(NbtIo.readCompressed(stream));

				if (tag.contains("id")) {
					id = UUIDTypeAdapter.fromString(tag.getString("id"));
				}

				// read some data, I guess?
			} catch (Exception ex) {
				ex.printStackTrace();
			}
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
					for (Path file : Files.list(dir).filter(path -> path.getFileName().toString().endsWith(".nbt")).collect(Collectors.toList())) {
						try (InputStream stream = Files.newInputStream(file)) {
							CompoundTag nbt = Objects.requireNonNull(NbtIo.readCompressed(stream));
							Team team = type.factory.apply(this);
							team.id = UUIDTypeAdapter.fromString(nbt.getString("id"));
							teamMap.put(team.id, team);
							team.deserializeNBT(nbt);
						} catch (Exception ex) {
							ex.printStackTrace();
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

		playerTeamMap.putAll(knownPlayers);

		for (Team team : teamMap.values()) {
			if (team instanceof PartyTeam) {
				for (UUID member : team.getMembers()) {
					playerTeamMap.put(member, team);
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
			try (OutputStream stream = Files.newOutputStream(directory.resolve("ftbteams.nbt"))) {
				NbtIo.writeCompressed(serializeNBT(), stream);
				shouldSave = false;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
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
				Path path = directory.resolve(team.getType().getSerializedName() + "/" + UUIDTypeAdapter.fromUUID(team.getId()) + ".nbt");

				try (OutputStream stream = Files.newOutputStream(path)) {
					NbtIo.writeCompressed(team.serializeNBT(), stream);
					team.shouldSave = false;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.putString("id", UUIDTypeAdapter.fromUUID(getId()));
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

	public void playerLoggedIn(ServerPlayer player) {
		UUID id = player.getUUID();
		PlayerTeam team = knownPlayers.get(id);

		if (team == null) {
			team = new PlayerTeam(this);
			team.id = id;
			team.playerName = player.getGameProfile().getName();
			teamMap.put(id, team);
			knownPlayers.put(id, team);
			playerTeamMap.put(id, team);

			team.setProperty(Team.DISPLAY_NAME, team.playerName);
			team.setProperty(Team.COLOR, FTBTUtils.randomColor());

			team.created(player);
			team.ranks.put(id, TeamRank.OWNER);
			team.changedTeam(Optional.empty(), id);
			team.save();
			save();
		}

		if (!team.playerName.equals(player.getGameProfile().getName())) {
			team.playerName = player.getGameProfile().getName();
			team.save();
		}

		sync(player, team.getId());
	}

	public ClientTeamManager createClientTeamManager() {
		ClientTeamManager clientManager = new ClientTeamManager(getId());

		for (Team team : getTeams()) {
			ClientTeam t = new ClientTeam(clientManager, team);
			clientManager.teamMap.put(t.getId(), t);

			if (team instanceof PlayerTeam) {
				clientManager.profileMap.put(team.getId(), new GameProfile(team.getId(), ((PlayerTeam) team).playerName));
			}
		}

		return clientManager;
	}

	public void sync(ServerPlayer player, UUID self) {
		new MessageSyncTeams(createClientTeamManager(), self).sendTo(player);
	}

	public void sync(ServerPlayer player) {
		sync(player, getPlayerTeam(player).getId());
	}

	public void syncAll() {
		ClientTeamManager clientManager = createClientTeamManager();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			new MessageSyncTeams(clientManager, getPlayerTeam(player).getId()).sendTo(player);
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
		playerTeamMap.put(id, team);

		team.ranks.put(id, TeamRank.OWNER);
		team.changedTeam(Optional.of(oldTeam), id);
		team.sendMessage(FTBTUtils.NO_PROFILE, new TextComponent("").append(player.getName()).append(" joined your party!").withStyle(ChatFormatting.YELLOW));
		team.save();

		oldTeam.ranks.remove(id);
		oldTeam.save();
		syncAll();
		return Pair.of(Command.SINGLE_SUCCESS, team);
	}

	public Pair<Integer, Team> leaveParty(ServerPlayer player) throws CommandSyntaxException {
		UUID id = player.getUUID();
		Team oldTeam = getPlayerTeam(player);

		if (oldTeam.getType().isPlayer()) {
			throw TeamArgument.NOT_IN_PARTY.create();
		}

		PlayerTeam team = getInternalPlayerTeam(id);
		playerTeamMap.put(id, team);

		team.ranks.put(id, TeamRank.OWNER);
		team.changedTeam(Optional.of(oldTeam), id);
		oldTeam.sendMessage(FTBTUtils.NO_PROFILE, new TextComponent("").append(player.getName()).append(" left your party!").withStyle(ChatFormatting.YELLOW));
		team.save();

		oldTeam.ranks.remove(id);
		oldTeam.save();

		if (oldTeam.getMembers().isEmpty()) {
			TeamDeletedEvent.EVENT.invoker().accept(new TeamDeletedEvent(oldTeam));
			saveNow();
			teamMap.remove(team.id);

			try {
				Path dir = server.getWorldPath(FOLDER_NAME).resolve("deleted");

				if (Files.notExists(dir)) {
					Files.createDirectories(dir);
				}

				String fn = UUIDTypeAdapter.fromUUID(oldTeam.id) + ".nbt";
				Files.move(server.getWorldPath(FOLDER_NAME).resolve("party/" + fn), dir.resolve(fn));
			} catch (IOException e) {
				e.printStackTrace();
			}

			save();
		}

		syncAll();
		return Pair.of(Command.SINGLE_SUCCESS, team);
	}

	public Pair<Integer, ServerTeam> createServer(CommandSourceStack source, String name) throws CommandSyntaxException {
		ServerTeam team = createServerTeam(source.getPlayerOrException(), name);
		source.sendSuccess(new TextComponent("Created new server team ").append(team.getName()), true);
		syncAll();
		return Pair.of(Command.SINGLE_SUCCESS, team);
	}

	public int deleteServer(CommandSourceStack source, Team team) throws CommandSyntaxException {
		if (!team.getType().isServer()) {
			source.sendFailure(new TextComponent("Can only delete a server team!"));
			return 0;
		}

		TeamDeletedEvent.EVENT.invoker().accept(new TeamDeletedEvent(team));
		saveNow();
		teamMap.remove(team.id);

		try {
			Path dir = server.getWorldPath(FOLDER_NAME).resolve("deleted");

			if (Files.notExists(dir)) {
				Files.createDirectories(dir);
			}

			String fn = UUIDTypeAdapter.fromUUID(team.id) + ".nbt";
			Files.move(server.getWorldPath(FOLDER_NAME).resolve("server/" + fn), dir.resolve(fn));
		} catch (IOException e) {
			e.printStackTrace();
		}

		source.sendSuccess(new TextComponent("Team deleted"), true);
		save();
		syncAll();
		return Command.SINGLE_SUCCESS;
	}
}