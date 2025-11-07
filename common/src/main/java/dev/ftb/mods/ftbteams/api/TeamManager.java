package dev.ftb.mods.ftbteams.api;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Top-level interface for querying team data on the server. Retrieve an instance of this via
 * {@link FTBTeamsAPI.API#getManager()}.
 */
public interface TeamManager {
    /**
     * Convenience method to get the current Minecraft server.
     *
     * @return the server
     */
    MinecraftServer getServer();

    /**
     * Get the unique ID for this team manager, which is a random UUID assigned on first creation.
     *
     * @return the unique manager ID
     */
    UUID getId();

    /**
     * Get an immutable collection of all teams known to the team manager. Note that this is a copy of the teams;
     * avoid excessive method calls.
     *
     * @return an immutable view of the known teams
     */
    Collection<Team> getTeams();

    /**
     * Get the team for the given player ID, if it exists.
     *
     * @param uuid a player UUID
     * @return the player's team (maybe a party team), or {@code Optional.empty()} if no team could be found
     */
    Optional<Team> getTeamForPlayerID(UUID uuid);

    /**
     * Get the team for the given player, if it exists.
     *
     * @param player a server-side player object
     * @return the player's team (maybe a party team), or {@code Optional.empty()} if no team could be found
     */
    Optional<Team> getTeamForPlayer(ServerPlayer player);

    /**
     * Get the player's own team, given their player ID. This always returns the player's personal team, even if
     * they are currently in a party team.
     *
     * @param uuid the player's UUID
     * @return the player's personal team, or {@code Optional.empty()} if no team could be found
     */
    Optional<Team> getPlayerTeamForPlayerID(UUID uuid);

    /**
     * Retrieve the given team by short (friendly) name. This is the name as returned by {@link Team#getShortName()}.
     *
     * @param teamName a team name
     * @return the team, or {@code Optional.empty()} if no team could be found
     */
    Optional<Team> getTeamByName(String teamName);

    /**
     * Retrieve the given team by its unique ID. This is the name as returned by {@link Team#getId()}.
     *
     * @param teamId unique team ID
     * @return the team, or {@code Optional.empty()} if no team could be found
     */
    Optional<Team> getTeamByID(UUID teamId);

    /**
     * Convenience method to check if two player IDs are in the same team.
     *
     * @param id1 UUID of the first player
     * @param id2 UUID of the first player
     * @return true if the players are in the same team
     */
    boolean arePlayersInSameTeam(UUID id1, UUID id2);

    /**
     * Get an unmodifiable map of player UUID to Team object for all known player teams. Note that this does not
     * include party teams.
     *
     * @return all known player teams
     */
    Map<UUID, ? extends Team> getKnownPlayerTeams();

    /**
     * Get any extension data that may exist in this team manager. This is empty by default, but other mods can use
     * this to store manager-specific data where necessary.
     * <p>
     * This data is serialized along with the rest of the manager so persists across server restarts, but if you change
     * any data in the compound tag returned by this method, you should call {@link #markDirty()} to ensure your changes
     * actually get saved.
     *
     * @return extension data for the manager
     */
    CompoundTag getExtraData();

    /**
     * Mark the manager as requiring serialization. The only time this should be necessary to call is if you change
     * any data in the compound returned by {@link #getExtraData()}.
     */
    void markDirty();

    /**
     * Attempt to create a party team for the given player.
     *
     * @param player the player to create the team for
     * @param name the human-readable team name (something like "{player}'s team" is suggested)
     * @param description a text description of the team, may be null
     * @param color a color definition; if null, a random color will be picked
     * @return the new party team
     * @throws CommandSyntaxException if there was any kind of failure creating the team,
     * most commonly that the player is already in a party
     */
    Team createPartyTeam(ServerPlayer player, String name, @Nullable String description, @Nullable Color4I color) throws CommandSyntaxException;

    /**
     * Attempt to create a server team for the given command source stack.
     *
     * @param commandSourceStack the command source, typically either {@link Player#createCommandSourceStack()} or {@link MinecraftServer#createCommandSourceStack()}
     * @param name the server team's displayed name
     * @param description the server team's description, may be null
     * @param color the color to use, may be null (if null, a random color is picked)
     * @param teamUUID the team's UUID, may be null (if null, a new random UUID is picked)
     * @return the new server team
     * @throws CommandSyntaxException if there was any kind of failure creating the team,
     * most commonly that a team with the given UUID already exists
     */
    Team createServerTeam(CommandSourceStack commandSourceStack, String name, @Nullable String description, @Nullable Color4I color, @Nullable UUID teamUUID) throws CommandSyntaxException;

    /**
     * See {@link #createServerTeam(CommandSourceStack, String, String, Color4I, UUID)}. This variant passes a null UUID,
     * indicating that a random UUID should be used for the new server team ID.
     */
    default Team createServerTeam(CommandSourceStack commandSourceStack, String name, @Nullable String description, @Nullable Color4I color) throws CommandSyntaxException {
        return createServerTeam(commandSourceStack, name, description, color, null);
    }
}
