package dev.ftb.mods.ftbteams;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.config.ServerConfig;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// Methods to facilitate sync'ing FTB party teams up with vanilla scoreboard teams
public class ScoreboardTeamHelper {

    public static final String TEAM_PREFIX = FTBTeamsAPI.MOD_ID + "-";

    /// Called on server startup to ensure that every party team has a corresponding vanilla team
    /// associated with it. Also remove any orphaned vanilla teams (where the FTB team has gone).
    /// Mainly a sanity check, since vanilla teams should be kept in sync under normal operation.
    public static void checkAllTeams(@Nullable TeamManager manager) {
        if (ServerConfig.SCOREBOARD_TEAM_SYNC.get() && manager != null) {
            ServerScoreboard scoreboard = manager.getServer().getScoreboard();

            // ensure that a scoreboard team is present for all known FTB party teams
            manager.getTeams().forEach(ftbTeam -> {
                if (ftbTeam.isPartyTeam()) {
                    String teamName = makeScoreboardTeamName(ftbTeam);
                    var sbTeam = scoreboard.getPlayerTeam(teamName);
                    if (sbTeam == null) {
                        createNewScoreboardTeam(manager.getServer(), ftbTeam, null);
                    }
                }
            });

            // ensure that any orphaned scoreboard teams (i.e. missing FTB party team) are removed
            List<PlayerTeam> toRemove = new ArrayList<>();
            scoreboard.getPlayerTeams().forEach(sbTeam -> {
                if (sbTeam.getName().startsWith(TEAM_PREFIX)) {
                    String idStr = sbTeam.getName().substring(TEAM_PREFIX.length());
                    try {
                        UUID id = UUID.fromString(idStr);
                        if (manager.getTeamByID(id).isEmpty()) {
                            // missing FTB team for this scoreboard team
                            toRemove.add(sbTeam);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            });
            toRemove.forEach(scoreboard::removePlayerTeam);
        }
    }

    /// Called on player login to ensure the player is a member of a scoreboard team, if they're in a FTB party team.
    public static void checkPlayer(TeamManagerImpl manager, ServerPlayer player) {
        if (ServerConfig.SCOREBOARD_TEAM_SYNC.get()) {
            manager.getTeamForPlayer(player).ifPresent(ftbTeam -> {
                if (ftbTeam.isPartyTeam()) {
                    var sbTeam = manager.getServer().getScoreboard().getPlayersTeam(player.getScoreboardName());
                    if (sbTeam == null || !sbTeam.getName().equals(makeScoreboardTeamName(ftbTeam))) {
                        addPlayerToTeam(manager.getServer(), ftbTeam, player.nameAndId());
                    }
                }
            });
        }
    }

    /// Called when a team's properties have changed; sync any relevant property data to the scoreboard team
    public static void onPropertiesChanged(MinecraftServer server, Team ftbTeam) {
        if (ServerConfig.SCOREBOARD_TEAM_SYNC.get()) {
            var sbTeam = getScoreboardTeam(server, ftbTeam);
            if (sbTeam != null) {
                syncTeamProps(ftbTeam, sbTeam);
            }
        }
    }

    @Nullable
    public static PlayerTeam getScoreboardTeam(MinecraftServer server, Team ftbTeam) {
        return server.getScoreboard().getPlayerTeam(makeScoreboardTeamName(ftbTeam));
    }

    public static void removeScoreboardTeam(MinecraftServer server, Team ftbTeam) {
        if (ServerConfig.SCOREBOARD_TEAM_SYNC.get()) {
            PlayerTeam team = server.getScoreboard().getPlayerTeam(makeScoreboardTeamName(ftbTeam));
            if (team != null) {
                server.getScoreboard().removePlayerTeam(team);
                FTBTeams.LOGGER.info("removed scoreboard team {} (FTB Team id {})", team.getName(), ftbTeam.getShortName());
            }
        }
    }

    public static void addPlayerToTeam(MinecraftServer server, Team ftbTeam, NameAndId playerProfile) {
        if (ServerConfig.SCOREBOARD_TEAM_SYNC.get()) {
            var playerTeam = getScoreboardTeam(server, ftbTeam);
            if (playerTeam == null) {
                // also handles adding the player
                createNewScoreboardTeam(server, ftbTeam, playerProfile);
            } else {
                server.getScoreboard().addPlayerToTeam(playerProfile.name(), playerTeam);
                FTBTeams.LOGGER.info("added player {} to scoreboard team {} (FTB Team id {})", playerProfile.name(), playerTeam.getName(), ftbTeam.getShortName());
            }
        }
    }

    public static void addPlayerToTeam(MinecraftServer server, Team team, UUID playerId) {
        if (ServerConfig.SCOREBOARD_TEAM_SYNC.get()) {
            server.services().profileResolver().fetchById(playerId).ifPresent(profile ->
                    addPlayerToTeam(server, team, new NameAndId(profile))
            );
        }
    }

    public static void removePlayerFromTeam(MinecraftServer server, UUID id, Team ftbTeam) {
        if (ServerConfig.SCOREBOARD_TEAM_SYNC.get()) {
            var playerTeam = getScoreboardTeam(server, ftbTeam);
            if (playerTeam != null) {
                server.services().profileResolver().fetchById(id).ifPresent(profile -> {
                            server.getScoreboard().removePlayerFromTeam(profile.name(), playerTeam);
                            FTBTeams.LOGGER.info("removed player {} from scoreboard team {} (FTB Team id {})", profile.name(), playerTeam.getName(), ftbTeam.getShortName());
                        }
                );
            }
        }
    }

    private static void syncTeamProps(Team ftbTeam, PlayerTeam sbTeam) {
        sbTeam.setDisplayName(Component.literal(ftbTeam.getShortName()));
        sbTeam.setColor(getBestColor(ftbTeam.getProperty(TeamProperties.COLOR)));

        sbTeam.setAllowFriendlyFire(ServerConfig.SCOREBOARD_FRIENDLY_FIRE.get());
        sbTeam.setSeeFriendlyInvisibles(ServerConfig.SCOREBOARD_FRIENDLY_INVIS.get());
        sbTeam.setCollisionRule(ServerConfig.SCOREBOARD_COLLISIONS.get());
        sbTeam.setDeathMessageVisibility(ServerConfig.SCOREBOARD_DEATH_MESSAGE_VISIBILITY.get());
        sbTeam.setNameTagVisibility(ServerConfig.SCOREBOARD_NAMETAG_VISIBILITY.get());
    }

    public static void resync() {
        if (ServerConfig.SCOREBOARD_TEAM_SYNC.get()) {
            TeamManager manager = FTBTeamsAPI.api().getManager();
            manager.getTeams().forEach(team -> {
                if (team.isPartyTeam()) {
                   onPropertiesChanged(manager.getServer(), team);
                }
            });
        }
    }

    private static void createNewScoreboardTeam(MinecraftServer server, Team ftbTeam, @Nullable NameAndId teamOwner) {
        if (ServerConfig.SCOREBOARD_TEAM_SYNC.get()) {
            String teamName = makeScoreboardTeamName(ftbTeam);
            var sbTeam = server.getScoreboard().addPlayerTeam(teamName);

            // set up initial team properties
            syncTeamProps(ftbTeam, sbTeam);

            // add team owner to scoreboard team
            if (teamOwner != null) {
                server.getScoreboard().addPlayerToTeam(teamOwner.name(), sbTeam);
            }
            // add any other members to scoreboard team
            ftbTeam.getMembers().forEach(id -> {
                if (teamOwner == null || !teamOwner.id().equals(id)) {
                    server.services().profileResolver().fetchById(id).ifPresent(profile ->
                            server.getScoreboard().addPlayerToTeam(profile.name(), sbTeam)
                    );
                }
            });

            FTBTeams.LOGGER.info("created new scoreboard team {} for FTB Team {}", sbTeam.getName(), ftbTeam.getShortName());
        }
    }

    private static String makeScoreboardTeamName(Team ftbTeam) {
        return TEAM_PREFIX + ftbTeam.getTeamId();
    }

    private static ChatFormatting getBestColor(Color4I target) {
        double best = Double.MAX_VALUE;
        ChatFormatting res = ChatFormatting.WHITE;
        for (var cf : ChatFormatting.values()) {
            if (cf.getColor() != null) {
                Color4I col1 = Color4I.rgb(cf.getColor());
                double dist = yCbCrColorDistance(col1, target);
                if (dist < best) {
                    best = dist;
                    res = cf;
                }
            }
        }
        return res;
    }

    private static double yCbCrColorDistance(Color4I c1, Color4I c2) {
        double[] result1 = chrominanceLuminance(c1);
        double[] result2 = chrominanceLuminance(c2);
        double deltaLuminance = result1[0] - result2[0];
        double delta_Cb = result1[1] - result2[1];
        double delta_Cr = result1[2] - result2[2];
        return Math.sqrt(deltaLuminance * deltaLuminance + delta_Cb * delta_Cb + delta_Cr * delta_Cr);
    }

    private static double[] chrominanceLuminance(Color4I color) {
        double luminance = 0.299 * color.redf() + 0.587 * color.greenf() + 0.114 * color.bluef();
        double Cb = 0.564 * (color.bluef() - luminance);
        double Cr = 0.713 * (color.redf() - luminance);
        return new double[]{luminance, Cb, Cr};
    }
}
