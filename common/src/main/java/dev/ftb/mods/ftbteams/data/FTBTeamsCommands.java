package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbteams.FTBTeamsAPIImpl;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamInfoEvent;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

public class FTBTeamsCommands {
	private Predicate<CommandSourceStack> requiresOPorSP() {
		return source -> source.getServer().isSingleplayer() || source.hasPermission(2);
	}

	private RequiredArgumentBuilder<CommandSourceStack, TeamArgumentProvider> teamArg() {
		return Commands.argument("team", TeamArgument.create());
	}

	private String string(CommandContext<?> context, String name) {
		return StringArgumentType.getString(context, name);
	}

	private boolean hasNoParty(CommandSourceStack source) {
		if (source.getEntity() instanceof ServerPlayer) {
			return FTBTeamsAPI.api().getManager().getTeamForPlayerID(source.getEntity().getUUID())
					.map(team -> !team.isPartyTeam())
					.orElse(false);
		}

		return false;
	}

	private boolean hasParty(CommandSourceStack source, TeamRank rank) {
		if (source.getEntity() instanceof ServerPlayer) {
			UUID playerId = source.getEntity().getUUID();
			return FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerId)
					.map(team -> team.isPartyTeam() && team.getRankForPlayer(playerId).isAtLeast(rank))
					.orElse(false);
		}

		return false;
	}

	private Team getTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
				.orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(player.getUUID()));
	}

	private PartyTeam getPartyTeam(CommandContext<CommandSourceStack> context, TeamRank minRank) throws CommandSyntaxException {
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

	private Team teamArg(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return TeamArgument.get(context, "team");
	}

	private Team teamArg(CommandContext<CommandSourceStack> context, Predicate<Team> predicate) throws CommandSyntaxException {
		Team team = teamArg(context);

		if (!predicate.test(team)) {
			throw TeamArgument.TEAM_NOT_FOUND.create(team.getName());
		}

		return team;
	}

	private ServerTeam serverTeamArg(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return (ServerTeam) teamArg(context, Team::isServerTeam);
	}

	private PartyTeam partyTeamArg(CommandContext<CommandSourceStack> context, TeamRank rank) throws CommandSyntaxException {
		PartyTeam team = (PartyTeam) teamArg(context, Team::isPartyTeam);

		if (rank != TeamRank.NONE && !team.getRankForPlayer(context.getSource().getPlayerOrException().getUUID()).isAtLeast(rank)) {
			throw TeamArgument.NOT_INVITED.create(team.getName());
		}

		return team;
	}

	private int tryCreateParty(CommandSourceStack source, String partyName) throws CommandSyntaxException {
		if (FTBTeamsAPIImpl.INSTANCE.isPartyCreationFromAPIOnly()) {
			throw TeamArgument.API_OVERRIDE.create();
		}
		return TeamManagerImpl.INSTANCE.createParty(source.getPlayerOrException(), partyName).getLeft();
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ftbteams")
				.then(Commands.literal("party")
						.then(Commands.literal("create")
								.requires(this::hasNoParty)
								.then(Commands.argument("name", StringArgumentType.greedyString())
										.executes(ctx -> tryCreateParty(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
								)
								.executes(ctx -> tryCreateParty(ctx.getSource(), ""))
						)
						.then(Commands.literal("join")
								.requires(this::hasNoParty)
								.then(teamArg()
										.executes(ctx -> partyTeamArg(ctx, TeamRank.INVITED).join(ctx.getSource().getPlayerOrException()))
								)
						)
						.then(Commands.literal("decline")
								.requires(this::hasNoParty)
								.then(teamArg()
										.executes(ctx -> partyTeamArg(ctx, TeamRank.INVITED).declineInvitation(ctx.getSource()))
								)
						)
						.then(Commands.literal("leave")
								.requires(source -> hasParty(source, TeamRank.MEMBER))
								.executes(ctx -> getPartyTeam(ctx, TeamRank.MEMBER).leave(ctx.getSource().getPlayerOrException().getUUID()))
						)
						.then(Commands.literal("invite")
								.requires(source -> hasParty(source, TeamRank.OFFICER))
								.then(Commands.argument("players", GameProfileArgument.gameProfile())
										.executes(ctx -> getPartyTeam(ctx, TeamRank.OFFICER).invite(ctx.getSource().getPlayerOrException(), GameProfileArgument.getGameProfiles(ctx, "players")))
								)
						)
						.then(Commands.literal("kick")
								.requires(source -> hasParty(source, TeamRank.OFFICER))
								.then(Commands.argument("players", GameProfileArgument.gameProfile())
										.executes(ctx -> getPartyTeam(ctx, TeamRank.OFFICER).kick(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "players")))
								)
						)
						.then(Commands.literal("transfer_ownership")
								.requires(source -> hasParty(source, TeamRank.OWNER))
								.then(Commands.argument("player_id", GameProfileArgument.gameProfile())
										.executes(ctx -> partyTeamArg(ctx, TeamRank.OWNER).transferOwnership(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player_id")))
								)
						)
						.then(Commands.literal("transfer_ownership_for")
								.requires(requiresOPorSP())
								.then(teamArg()
										.then(Commands.argument("player_id", GameProfileArgument.gameProfile())
												.executes(ctx -> partyTeamArg(ctx, TeamRank.NONE).transferOwnership(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player_id")))
										)
								)
						)
						.then(Commands.literal("settings")
								.requires(source -> hasParty(source, TeamRank.OWNER))
								.then(Commands.argument("key", TeamPropertyArgument.create())
										.then(Commands.argument("value", StringArgumentType.greedyString())
												.executes(ctx -> getPartyTeam(ctx, TeamRank.OWNER).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), string(ctx, "value")))
										)
										.executes(ctx -> getPartyTeam(ctx, TeamRank.OWNER).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), ""))
								)
						)
						.then(Commands.literal("settings_for")
								.requires(requiresOPorSP())
								.then(teamArg()
										.then(Commands.argument("key", TeamPropertyArgument.create())
												.then(Commands.argument("value", StringArgumentType.greedyString())
														.executes(ctx -> partyTeamArg(ctx, TeamRank.OFFICER).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), string(ctx, "value")))
												)
												.executes(ctx -> partyTeamArg(ctx, TeamRank.OFFICER).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), ""))
										)
								)
						)
						.then(Commands.literal("allies")
								.requires(source -> hasParty(source, TeamRank.MEMBER))
								.then(Commands.literal("add")
										.requires(source -> hasParty(source, TeamRank.OFFICER))
										.then(Commands.argument("player", GameProfileArgument.gameProfile())
												.executes(ctx -> getPartyTeam(ctx, TeamRank.OFFICER).addAlly(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player")))
										)
								)
								.then(Commands.literal("remove")
										.requires(source -> hasParty(source, TeamRank.OFFICER))
										.then(Commands.argument("player", GameProfileArgument.gameProfile())
												.executes(ctx -> getPartyTeam(ctx, TeamRank.OFFICER).removeAlly(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player")))
										)
								)
								.then(Commands.literal("list")
										.requires(source -> hasParty(source, TeamRank.MEMBER))
										.executes(ctx -> getPartyTeam(ctx, TeamRank.MEMBER).listAllies(ctx.getSource()))
								)
						)
				)
				.then(Commands.literal("server")
						.requires(requiresOPorSP())
						.then(Commands.literal("create")
								.then(Commands.argument("name", StringArgumentType.greedyString())
										.executes(ctx -> TeamManagerImpl.INSTANCE.createServer(ctx.getSource(), string(ctx, "name")).getLeft())
								)
						)
						.then(Commands.literal("delete")
								.then(teamArg()
										.executes(ctx -> serverTeamArg(ctx).delete(ctx.getSource()))
								)
						)
						.then(Commands.literal("settings")
								.then(teamArg()
										.then(Commands.argument("key", TeamPropertyArgument.create())
												.then(Commands.argument("value", StringArgumentType.greedyString())
														.executes(ctx -> serverTeamArg(ctx).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), string(ctx, "value")))
												)
												.executes(ctx -> serverTeamArg(ctx).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), ""))
										)
								)
						)
				)
				.then(Commands.literal("msg")
						.then(Commands.argument("text", StringArgumentType.greedyString())
								.executes(ctx -> {
									getTeam(ctx).sendMessage(ctx.getSource().getPlayerOrException().getUUID(), StringArgumentType.getString(ctx, "text"));
									return Command.SINGLE_SUCCESS;
								})
						)
				)
				.then(Commands.literal("info")
						.then(Commands.literal("server_id")
								.executes(ctx -> serverId(ctx.getSource()))
						)
						.then(teamArg()
								.executes(ctx -> info(ctx.getSource(), teamArg(ctx)))
						)
						.executes(ctx -> info(ctx.getSource(), getTeam(ctx)))
				)
				.then(Commands.literal("list")
						.executes(ctx -> list(ctx.getSource(), null))
						.then(Commands.literal("parties")
								.executes(ctx -> list(ctx.getSource(), Team::isPartyTeam))
						)
						.then(Commands.literal("server_teams")
								.executes(ctx -> list(ctx.getSource(), Team::isServerTeam))
						)
						.then(Commands.literal("players")
								.executes(ctx -> list(ctx.getSource(), Team::isPlayerTeam))
						)
				)
				.then(Commands.literal("force-disband")
						.requires(source -> source.hasPermission(2))
						.then(teamArg()
								.executes(ctx -> partyTeamArg(ctx, TeamRank.NONE).forceDisband(ctx.getSource()))
						)
				)
		);

		if (Platform.isDevelopmentEnvironment()) {
			dispatcher.register(Commands.literal("ftbteams_add_fake_player")
					.requires(source -> source.hasPermission(2))
					.then(Commands.argument("profile", GameProfileArgument.gameProfile())
							.executes(ctx -> addFakePlayer(GameProfileArgument.getGameProfiles(ctx, "profile")))
					)
			);
		}
	}

	private int info(CommandSourceStack source, Team team) {
		team.getTeamInfo().forEach(line -> source.sendSuccess(() -> line, false));

		TeamEvent.INFO.invoker().accept(new TeamInfoEvent(team, source));

		return Command.SINGLE_SUCCESS;
	}

	private int serverId(CommandSourceStack source) {
		UUID managerId = FTBTeamsAPI.api().getManager().getId();
		MutableComponent component = Component.literal("Server ID: " + managerId);
		component.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy"))));
		component.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, managerId.toString())));
		source.sendSuccess(() -> component, false);
		return 1;
	}

	private int list(CommandSourceStack source, @Nullable Predicate<Team> predicate) {
		MutableComponent list = Component.literal("");

		boolean first = true;

		for (Team team : FTBTeamsAPI.api().getManager().getTeams()) {
			if (predicate != null && !predicate.test(team)) {
				continue;
			}

			if (first) {
				first = false;
			} else {
				list.append(", ");
			}

			list.append(team.getName());
		}

		final Component msg = Component.translatable("ftbteams.list", first ? Component.translatable("ftbteams.info.owner.none") : list);
		source.sendSuccess(() -> msg, false);
		return Command.SINGLE_SUCCESS;
	}

	private int addFakePlayer(Collection<GameProfile> profiles) {
		for (GameProfile profile : profiles) {
			TeamManagerImpl.INSTANCE.playerLoggedIn(null, profile.getId(), profile.getName());
		}

		return 1;
	}
}
