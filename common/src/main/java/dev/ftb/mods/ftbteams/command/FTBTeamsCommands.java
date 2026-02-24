package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.ServerTeam;
import dev.ftb.mods.ftbteams.data.TeamType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

public class FTBTeamsCommands {
	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(FTBTeamsAPI.MOD_ID)
						.then(AddFakePlayerCommand.register())
						.then(ForceCommand.register())
						.then(InfoCommand.register())
						.then(ListCommand.register())
						.then(MessageCommand.register())
						.then(NbtEditCommand.register())
						.then(PartyTeamCommand.register())
						.then(RedirectChatCommand.register())
						.then(ServerTeamCommand.register())
						.then(TeamStageCommand.register())
		);
	}

	static Predicate<CommandSourceStack> requiresOPorSP() {
		return source -> source.getServer().isSingleplayer() || source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	static RequiredArgumentBuilder<CommandSourceStack, TeamArgumentProvider> createTeamArg() {
		return createTeamArg(null);
	}

	static RequiredArgumentBuilder<CommandSourceStack, TeamArgumentProvider> createTeamArg(@Nullable TeamType type) {
		return Commands.argument("team", TeamArgument.create(type));
	}

	static String string(CommandContext<?> context, String name) {
		return StringArgumentType.getString(context, name);
	}

	static boolean hasNoParty(CommandSourceStack source) {
		if (source.getEntity() instanceof ServerPlayer) {
			return FTBTeamsAPI.api().getManager().getTeamForPlayerID(source.getEntity().getUUID())
					.map(team -> !team.isPartyTeam())
					.orElse(false);
		}

		return false;
	}

	static boolean hasParty(CommandSourceStack source, TeamRank rank) {
		if (source.getEntity() instanceof ServerPlayer) {
			UUID playerId = source.getEntity().getUUID();
			return FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerId)
					.map(team -> team.isPartyTeam() && team.getRankForPlayer(playerId).isAtLeast(rank))
					.orElse(false);
		}

		return false;
	}

	static Team getTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
				.orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(player.getUUID()));
	}

	static PartyTeam getPartyTeam(CommandContext<CommandSourceStack> context, TeamRank minRank) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Team team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
				.orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(player.getUUID()));

		if (!(team instanceof PartyTeam partyTeam)) {
			throw TeamArgument.NOT_IN_PARTY.create();
		}

		if (!partyTeam.getRankForPlayer(player.getUUID()).isAtLeast(minRank)) {
			throw TeamArgument.CANT_EDIT.create(team.getName());
		}

		return partyTeam;
	}

	static Team teamArg(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return TeamArgument.get(context, "team");
	}

	static ServerTeam serverTeamArg(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return (ServerTeam) teamArg(context, Team::isServerTeam);
	}

	static PartyTeam partyTeamArg(CommandContext<CommandSourceStack> context, TeamRank rank) throws CommandSyntaxException {
		PartyTeam team = (PartyTeam) teamArg(context, Team::isPartyTeam);

		if (rank != TeamRank.NONE && !team.getRankForPlayer(context.getSource().getPlayerOrException().getUUID()).isAtLeast(rank)) {
			throw TeamArgument.NOT_INVITED.create(team.getName());
		}

		return team;
	}

	private static Team teamArg(CommandContext<CommandSourceStack> context, Predicate<Team> predicate) throws CommandSyntaxException {
		Team team = teamArg(context);

		if (!predicate.test(team)) {
			throw TeamArgument.TEAM_NOT_FOUND.create(team.getName());
		}

		return team;
	}
}
