package dev.ftb.mods.ftbteams.api;

import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Utility class providing some convenience methods for querying and adjust the team stages for a team. These methods
 * all handle client sync and team data serialization internally.
 * <p>
 * Modifying a team's stages also causes a {@link TeamPropertiesChangedEvent} event to be fired.
 */
public class TeamStagesHelper {
    /**
     * Add one team stage to a team.
     *
     * @param team the team
     * @param stage the stage to add
     * @return true if the stage was added, false if the team already had the stage
     */
    public static boolean addTeamStage(Team team, String stage) {
        return addTeamStages(team, List.of(stage)) == 1;
    }

    /**
     * Add multiple team stages to a team. It is much more efficient to use this method rather than repeated calls to
     * {@link #addTeamStage(Team, String)} when adding multiple stages.
     *
     * @param team the team
     * @param stages the stages to add
     * @return the number of stages actually added
     */
    public static int addTeamStages(Team team, Collection<String> stages) {
        return updateStages(team, stages, true);
    }

    /**
     * Remove one team stage from a team.
     *
     * @param team the team
     * @param stage the stage to add
     * @return true if the stage was remove, false if the team did not have the stage
     */
    public static boolean removeTeamStage(Team team, String stage) {
        return removeTeamStages(team, List.of(stage)) == 1;
    }

    /**
     * Removing multiple team stages from a team. It is much more efficient to use this method rather than repeated calls to
     * {@link #removeTeamStage(Team, String)} when removing multiple stages.
     *
     * @param team the team
     * @param stages the stages to add
     * @return the number of stages actually removed
     */
    public static int removeTeamStages(Team team, Collection<String> stages) {
        return updateStages(team, stages, false);
    }

    /**
     * Check if a team has a particular stage.
     *
     * @param team the team
     * @param stage the stage to check
     * @return true if the team has the stage, false otherwise
     */
    public static boolean hasTeamStage(Team team, String stage) {
        return team.getProperty(TeamProperties.TEAM_STAGES).contains(stage);
    }

    /**
     * Get all the stages for a team.
     *
     * @param team the team
     * @return an unmodifiable collection of the team's stages
     */
    public static Collection<String> getStages(Team team) {
        return Collections.unmodifiableSet(team.getProperty(TeamProperties.TEAM_STAGES));
    }

    private static int updateStages(Team team, Collection<String> stages, boolean adding) {
        Set<String> stageSet = team.getProperty(TeamProperties.TEAM_STAGES);
        int changed = (int) stages.stream().filter(stage -> adding && stageSet.add(stage) || !adding && stageSet.remove(stage)).count();
        if (changed > 0) {
            TeamPropertyCollection old = team.getProperties().copy();
            team.setProperty(TeamProperties.TEAM_STAGES, stageSet);
            TeamEvent.PROPERTIES_CHANGED.invoker().accept(new TeamPropertiesChangedEvent(team, old));
            team.syncOnePropertyToTeam(TeamProperties.TEAM_STAGES, stageSet);
        }

        return changed;
    }
}
