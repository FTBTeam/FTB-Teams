package dev.ftb.mods.ftbteams.config;

import dev.ftb.mods.ftblibrary.config.value.BooleanValue;
import dev.ftb.mods.ftblibrary.config.value.Config;
import dev.ftb.mods.ftblibrary.config.value.EnumValue;
import dev.ftb.mods.ftblibrary.config.value.IntValue;
import dev.ftb.mods.ftblibrary.util.NameMap;
import dev.ftb.mods.ftbteams.ScoreboardTeamHelper;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.world.scores.Team;

import java.util.OptionalInt;

public interface ServerConfig {
    String KEY = FTBTeamsAPI.MOD_ID + "-server";

    Config CONFIG = Config.create(KEY).standardTopLevelComment(FTBTeamsAPI.MOD_NAME, KEY, false);

    IntValue LIMITED_LIVES = CONFIG.addInt("limited_lives", 0, 0, Integer.MAX_VALUE)
            .comment("If >0, party teams have this many limited lives:",
                    "When members die, a party life is lost.",
                    "If a member dies when the party has no lives remaining, they are kicked from the party.",
                    "If the party owner dies, a random member is promoted to owner (officers have priority).",
                    "Parties with no lives remaining cannot invite new members."
            );

    IntValue MAX_TEAM_SIZE = CONFIG.addInt("max_party_size", 0, 0, Integer.MAX_VALUE)
            .comment("If >0, teams can contain no more than this number of players.",
                    "If 0, there is no limit to team sizes.",
                    "Note: if this setting is altered and teams larger than the new value already exist on the server",
                    "they will be allowed to continue at their current size, but not able to invite new members."
            );

    NameMap<Team.CollisionRule> COLLISION_RULE_NAME_MAP = NameMap.of(Team.CollisionRule.ALWAYS, Team.CollisionRule.values())
            .id(Team.CollisionRule::getSerializedName)
            .baseNameKey("team.collision")
            .create();
    NameMap<Team.Visibility> VISIBILITY_NAME_MAP = NameMap.of(Team.Visibility.ALWAYS, Team.Visibility.values())
            .id(Team.Visibility::getSerializedName)
            .baseNameKey("team.visibility")
            .create();

    Config SCOREBOARD_TEAM = CONFIG.addGroup("scoreboard_team")
            .comment("Settings for vanilla scoreboard team syncing");
    BooleanValue SCOREBOARD_TEAM_SYNC = SCOREBOARD_TEAM.addBoolean("team_sync_enabled", false)
            .comment("If true, a corresponding vanilla scoreboard team is created when a FTB Teams party team is created",
                    "Players will be added to both the FTB Teams team and the vanilla scoreboard team"
            );
    BooleanValue SCOREBOARD_FRIENDLY_FIRE = SCOREBOARD_TEAM.addBoolean("friendly_fire", true)
            .comment("Whether to allow friendly fire between players on the same scoreboard team");
    BooleanValue SCOREBOARD_FRIENDLY_INVIS = SCOREBOARD_TEAM.addBoolean("friendly_invis", true)
            .comment("Whether to allow players on the same scoreboard team to see each other if invisible");
    EnumValue<Team.CollisionRule> SCOREBOARD_COLLISIONS = SCOREBOARD_TEAM.addEnum("collisions", COLLISION_RULE_NAME_MAP)
            .comment("How collisions are handled between players on the same scoreboard team");
    EnumValue<Team.Visibility> SCOREBOARD_DEATH_MESSAGE_VISIBILITY = SCOREBOARD_TEAM.addEnum("death_messages", VISIBILITY_NAME_MAP)
            .comment("Death message handling");
    EnumValue<Team.Visibility> SCOREBOARD_NAMETAG_VISIBILITY = SCOREBOARD_TEAM.addEnum("nametag_visibility", VISIBILITY_NAME_MAP)
            .comment("Name tag visibility");

    static OptionalInt limitedLives() {
        return LIMITED_LIVES.get() > 0 ? OptionalInt.of(LIMITED_LIVES.get()) : OptionalInt.empty();
    }

    static boolean isPartyFull(int memberCount) {
        return MAX_TEAM_SIZE.get() != 0 && MAX_TEAM_SIZE.get() <= memberCount;
    }

    static void onChanged(boolean serverSide) {
        if (serverSide) {
            ScoreboardTeamHelper.resync();
        }
    }
}
