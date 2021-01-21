package com.feed_the_beast.mods.ftbteams.api;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author LatvianModder
 */
public interface TeamManager
{
	MinecraftServer getServer();

	Set<GameProfile> getKnownPlayers();

	Collection<Team> getTeams();

	IntSet getTeamIDs();

	Map<String, Team> getTeamNameMap();

	Optional<Team> getTeam(int id);

	Optional<Team> getTeam(GameProfile profile);

	default Optional<Team> getTeam(ServerPlayer player)
	{
		return getTeam(player.getGameProfile());
	}

	default boolean arePlayersInSameTeam(ServerPlayer player1, ServerPlayer player2)
	{
		return getTeam(player1).equals(getTeam(player2));
	}

	default int getTeamID(GameProfile profile)
	{
		Optional<Team> team = getTeam(profile);
		return team.map(Team::getId).orElse(0);
	}

	default int getTeamID(ServerPlayer player)
	{
		return getTeamID(player.getGameProfile());
	}

	Team newTeam();
}