package dev.ftb.mods.ftbteams.api;

import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import dev.ftb.mods.ftbteams.data.TeamRank;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class FTBTeamsAPI {
	public static Consumer<MouseButton> partyCreationOverride = null;

	public static boolean isManagerLoaded() {
		return TeamManager.INSTANCE != null;
	}

	public static TeamManager getManager() {
		return Objects.requireNonNull(TeamManager.INSTANCE);
	}

	public static boolean isClientManagerLoaded() {
		return ClientTeamManager.INSTANCE != null;
	}

	public static ClientTeamManager getClientManager() {
		return Objects.requireNonNull(ClientTeamManager.INSTANCE);
	}

	@Nullable
	public static Team getPlayerTeam(UUID profile) {
		return isManagerLoaded() ? getManager().getPlayerTeam(profile) : null;
	}

	public static Team getPlayerTeam(ServerPlayer player) {
		return getManager().getPlayerTeam(player);
	}

	public static UUID getPlayerTeamID(UUID profile) {
		return isManagerLoaded() ? getManager().getPlayerTeamID(profile) : profile;
	}

	public static boolean arePlayersInSameTeam(ServerPlayer player1, ServerPlayer player2) {
		return player1 == player2 || arePlayersInSameTeam(player1.getUUID(), player2.getUUID());
	}

	public static boolean arePlayersInSameTeam(UUID player1, UUID player2) {
		return getPlayerTeamID(player1).equals(getPlayerTeamID(player2));
	}

	public static TeamRank getHighestRank(UUID teamId, UUID player) {
		if (isManagerLoaded()) {
			Team team = getManager().getTeamByID(teamId);

			if (team != null) {
				return team.getHighestRank(player);
			}
		}

		return TeamRank.NONE;
	}
}