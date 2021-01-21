package com.feed_the_beast.mods.ftbteams.impl;

import com.feed_the_beast.mods.ftbteams.ProfileUtils;
import com.feed_the_beast.mods.ftbteams.api.Team;
import com.feed_the_beast.mods.ftbteams.api.TeamArgument;
import com.feed_the_beast.mods.ftbteams.api.TeamManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class TeamManagerImpl implements TeamManager, INBTSerializable<CompoundTag>
{
	public static final LevelResource FOLDER_NAME = new LevelResource("data/ftbteams");
	public static TeamManagerImpl instance;

	public final MinecraftServer server;
	private boolean shouldSave;
	private final Set<GameProfile> knownPlayers;
	public final Int2ObjectOpenHashMap<Team> teamMap;
	public final Map<GameProfile, TeamImpl> playerTeamMap;
	Map<String, Team> nameMap;
	int lastUID;

	public TeamManagerImpl(MinecraftServer s)
	{
		server = s;
		knownPlayers = new LinkedHashSet<>();
		teamMap = new Int2ObjectOpenHashMap<>();
		playerTeamMap = new HashMap<>();
		lastUID = 0;
	}

	@Override
	public MinecraftServer getServer()
	{
		return server;
	}

	@Override
	public Set<GameProfile> getKnownPlayers()
	{
		return knownPlayers;
	}

	@Override
	public Collection<Team> getTeams()
	{
		return teamMap.values();
	}

	@Override
	public IntSet getTeamIDs()
	{
		return teamMap.keySet();
	}

	@Override
	public Map<String, Team> getTeamNameMap()
	{
		if (nameMap == null)
		{
			nameMap = new HashMap<>();

			for (Team team : getTeams())
			{
				nameMap.put(team.getStringID(), team);
			}
		}

		return nameMap;
	}

	@Override
	public Optional<Team> getTeam(int id)
	{
		if (id <= 0)
		{
			return Optional.empty();
		}

		return Optional.ofNullable(teamMap.get(id));
	}

	@Override
	public Optional<Team> getTeam(GameProfile profile)
	{
		return Optional.ofNullable(playerTeamMap.get(profile));
	}

	@Override
	public int getTeamID(GameProfile profile)
	{
		Team team = playerTeamMap.get(profile);
		return team == null ? 0 : team.getId();
	}

	@Override
	public TeamImpl newTeam()
	{
		return new TeamImpl(this);
	}

	public void load()
	{
		File directory = server.getWorldPath(FOLDER_NAME).toFile();

		if (!directory.exists() || !directory.isDirectory())
		{
			return;
		}

		File dataFile = new File(directory, "ftbteams.nbt");

		if (dataFile.exists())
		{
			try
			{
				deserializeNBT(Objects.requireNonNull(NbtIo.read(dataFile)));
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		File[] files = directory.listFiles();

		if (files == null || files.length == 0)
		{
			return;
		}

		for (File file : files)
		{
			if (!file.isFile() || !file.getName().endsWith(".nbt") || file.getName().equals("ftbteams.nbt"))
			{
				continue;
			}

			try
			{
				CompoundTag nbt = Objects.requireNonNull(NbtIo.read(file));

				int id = nbt.getInt("id");

				if (id > 0)
				{
					TeamImpl team = new TeamImpl(this);
					team.id = id;
					teamMap.put(id, team);

					team.deserializeNBT(nbt);

					for (GameProfile member : team.members)
					{
						playerTeamMap.put(member, team);
					}
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	public void save()
	{
		shouldSave = true;
		nameMap = null;
	}

	public void saveAll()
	{
		File directory = server.getWorldPath(FOLDER_NAME).toFile();

		if (!directory.exists())
		{
			directory.mkdirs();
		}

		if (shouldSave)
		{
			try
			{
				NbtIo.write(serializeNBT(), new File(directory, "ftbteams.nbt"));
				shouldSave = false;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		for (Team team : getTeams())
		{
			try
			{
				((TeamImpl) team).saveFile(directory);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	@Override
	public CompoundTag serializeNBT()
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("last_id", lastUID);

		ListTag knownPlayersNBT = new ListTag();

		for (GameProfile profile : knownPlayers)
		{
			knownPlayersNBT.add(StringTag.valueOf(ProfileUtils.serializeProfile(profile)));
		}

		nbt.put("known_players", knownPlayersNBT);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt)
	{
		lastUID = nbt.getInt("last_id");

		knownPlayers.clear();
		ListTag knownPlayersNBT = nbt.getList("known_players", Constants.NBT.TAG_STRING);

		for (int i = 0; i < knownPlayersNBT.size(); i++)
		{
			GameProfile profile = ProfileUtils.deserializeProfile(knownPlayersNBT.getString(i));

			if (profile != ProfileUtils.NO_PROFILE)
			{
				knownPlayers.add(profile);
			}
		}
	}

	public Team createPlayerTeam(ServerPlayer player, String customName) throws CommandSyntaxException
	{
		Optional<Team> oldTeam = getTeam(player);

		if (oldTeam.isPresent())
		{
			throw TeamArgument.ALREADY_IN_TEAM.create();
		}

		TeamImpl team = newTeam();
		team.owner = player.getGameProfile();

		if (!customName.isEmpty())
		{
			team.setProperty(TeamImpl.DISPLAY_NAME, customName);
		}
		else
		{
			team.setProperty(TeamImpl.DISPLAY_NAME, player.getGameProfile().getName() + "'s Team");
		}

		team.setProperty(TeamImpl.COLOR, TeamImpl.randomColor(player.level.random));
		team.create();
		team.addMember(player);
		return team;
	}
}