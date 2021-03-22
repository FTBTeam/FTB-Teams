package com.feed_the_beast.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.shedaniel.architectury.hooks.LevelResourceHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class TeamManager {
	public static final LevelResource FOLDER_NAME = LevelResourceHooks.create("ftbteams");
	public static TeamManager INSTANCE;

	public final MinecraftServer server;
	private boolean shouldSave;
	final Map<GameProfile, PlayerTeam> knownPlayers;
	final Int2ObjectLinkedOpenHashMap<Team> teamMap;
	final Map<GameProfile, Team> playerTeamMap;
	Map<String, Team> nameMap;
	int lastUID;

	public TeamManager(MinecraftServer s) {
		server = s;
		knownPlayers = new LinkedHashMap<>();
		teamMap = new Int2ObjectLinkedOpenHashMap<>();
		playerTeamMap = new HashMap<>();
		lastUID = 0;
	}

	public MinecraftServer getServer() {
		return server;
	}

	public Map<GameProfile, PlayerTeam> getKnownPlayers() {
		return knownPlayers;
	}

	public Collection<Team> getTeams() {
		return teamMap.values();
	}

	public IntSet getTeamIDs() {
		return teamMap.keySet();
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
	public Team getTeam(int id) {
		if (id <= 0) {
			return null;
		}

		return teamMap.get(id);
	}

	@Nullable
	public Team getTeam(GameProfile profile) {
		return playerTeamMap.get(profile);
	}

	@Nullable
	public Team getTeam(ServerPlayer player) {
		return getTeam(player.getGameProfile());
	}

	public boolean arePlayersInSameTeam(ServerPlayer player1, ServerPlayer player2) {
		return getTeam(player1).equals(getTeam(player2));
	}

	public int getTeamID(GameProfile profile) {
		Team team = playerTeamMap.get(profile);
		return team == null ? 0 : team.getId();
	}

	public int getTeamID(ServerPlayer player) {
		return getTeamID(player.getGameProfile());
	}

	public void load() {
		Path directory = server.getWorldPath(FOLDER_NAME);

		if (Files.notExists(directory) || !Files.isDirectory(directory)) {
			return;
		}

		Path dataFile = directory.resolve("ftbteams.nbt");

		if (Files.exists(dataFile)) {
			try (InputStream stream = Files.newInputStream(dataFile)) {
				CompoundTag tag = Objects.requireNonNull(NbtIo.readCompressed(stream));
				lastUID = tag.getInt("last_id");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		for (TeamType type : TeamType.MAP.values()) {
			Path dir = directory.resolve(type.getSerializedName());

			if (Files.exists(dir) && Files.isDirectory(dir)) {
				try {
					for (Path file : Files.list(dir).filter(path -> path.getFileName().toString().endsWith(".nbt")).collect(Collectors.toList())) {
						try (InputStream stream = Files.newInputStream(file)) {
							CompoundTag nbt = Objects.requireNonNull(NbtIo.readCompressed(stream));

							int id = nbt.getInt("id");

							if (id > 0) {
								Team team = type.factory.apply(this);
								team.id = id;
								teamMap.put(id, team);

								team.deserializeNBT(nbt);

								for (GameProfile member : team.members) {
									playerTeamMap.put(member, team);
								}
							}
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
				knownPlayers.put(team.owner, (PlayerTeam) team);
			}
		}
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

		for (Team team : getTeams()) {
			if (team.shouldSave) {
				Path path = directory.resolve(team.getType().getSerializedName() + "/" + team.getId() + ".nbt");

				if (Files.notExists(path.getParent())) {
					try {
						Files.createDirectories(path.getParent());
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}

				try (OutputStream stream = Files.newOutputStream(path)) {
					NbtIo.writeCompressed(serializeNBT(), stream);
					team.shouldSave = false;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public CompoundTag serializeNBT() {
		CompoundTag nbt = new OrderedCompoundTag();
		nbt.putInt("last_id", lastUID);

		ListTag knownPlayersNBT = new ListTag();

		for (GameProfile profile : knownPlayers.keySet()) {
			knownPlayersNBT.add(StringTag.valueOf(FTBTUtils.serializeProfile(profile)));
		}

		nbt.put("known_players", knownPlayersNBT);
		return nbt;
	}

	public Team createPlayerTeam(TeamType type, GameProfile player, String customName) {
		Team team = type.factory.apply(this);
		team.owner = player;

		if (!customName.isEmpty()) {
			team.setProperty(Team.DISPLAY_NAME, customName);
		} else {
			team.setProperty(Team.DISPLAY_NAME, player.getName());
		}

		team.setProperty(Team.COLOR, FTBTUtils.randomColor());
		team.create();
		team.addMember(player);
		return team;
	}

	public Team createServerTeam(String name) {
		TeamManager manager = TeamManager.INSTANCE;
		Team team = TeamType.SERVER.factory.apply(manager);
		team.setProperty(Team.DISPLAY_NAME, name);
		team.setProperty(Team.COLOR, FTBTUtils.randomColor());
		team.create();
		return team;
	}

	public PlayerTeam getPlayerTeam(GameProfile profile) {
		return knownPlayers.get(profile);
	}

	public void playerLoggedIn(ServerPlayer player) {
		GameProfile profile = FTBTUtils.normalize(new GameProfile(player.getUUID(), player.getGameProfile().getName()));

		if (profile != FTBTUtils.NO_PROFILE && !knownPlayers.containsKey(profile)) {
			knownPlayers.put(profile, (PlayerTeam) createPlayerTeam(TeamType.PLAYER, player.getGameProfile(), ""));
			save();
		}
	}
}