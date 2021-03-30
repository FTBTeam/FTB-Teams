package dev.ftb.mods.ftbteams.data;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.property.TeamPropertyArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class FTBTeamsCommands {
	private Predicate<CommandSourceStack> requiresOPorSP() {
		return source -> source.getServer().isSingleplayer() || source.hasPermission(2);
	}

	private String string(CommandContext<?> context, String name) {
		return StringArgumentType.getString(context, name);
	}

	private Team team(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return FTBTeamsAPI.getManager().getPlayerTeam(player);
	}

	private Team teamAsOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Team team = FTBTeamsAPI.getManager().getPlayerTeam(player);

		//if (!team.isOwner(player)) {
		//	throw TeamArgument.NOT_OWNER.create(team.getName());
		//}

		return team;
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ftbteams")
				.then(Commands.literal("party")
						.then(Commands.literal("create")
								.then(Commands.argument("name", StringArgumentType.greedyString())
										.executes(ctx -> TeamManager.INSTANCE.createParty(ctx.getSource().getPlayerOrException(), string(ctx, "name")))
								)
								.executes(ctx -> TeamManager.INSTANCE.createParty(ctx.getSource().getPlayerOrException(), ""))
						)
						.then(Commands.literal("leave")
								.executes(ctx -> TeamManager.INSTANCE.leaveParty(ctx.getSource().getPlayerOrException()))
						)
				)
				.then(Commands.literal("server")
						.requires(requiresOPorSP())
						.then(Commands.literal("create")
								.then(Commands.argument("name", StringArgumentType.greedyString())
										.executes(ctx -> TeamManager.INSTANCE.createServer(ctx.getSource(), string(ctx, "name")))
								)
								.executes(ctx -> TeamManager.INSTANCE.createServer(ctx.getSource(), ""))
						)
						.then(Commands.literal("delete")
								.then(Commands.argument("id", TeamArgument.create())
										.executes(ctx -> TeamManager.INSTANCE.deleteServer(ctx.getSource(), TeamArgument.get(ctx, "id")))
								)
						)
						.then(Commands.literal("settings")
								.then(Commands.argument("team", TeamArgument.create())
										.then(Commands.argument("key", TeamPropertyArgument.create())
												.then(Commands.argument("value", StringArgumentType.greedyString())
														.executes(ctx -> TeamArgument.get(ctx, "team").settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), string(ctx, "value")))
												)
												.executes(ctx -> TeamArgument.get(ctx, "team").settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), ""))
										)
								)
						)
				)
		);

		/*
		dispatcher.register(Commands.literal("ftbteams")
				.then(Commands.literal("delete")
						.then(Commands.argument("team", FTBTeamsAPI.argument())
								.requires(requiresOPorSP())
								.executes(ctx -> team(ctx, "team").delete(ctx.getSource()))
						)
						.executes(ctx -> teamAsOwner(ctx).delete(ctx.getSource()))
				)
				.then(Commands.literal("settings")
						.then(Commands.argument("key", new TeamPropertyArgument())
								.then(Commands.argument("value", StringArgumentType.greedyString())
										.executes(ctx -> teamAsOwner(ctx).settings(ctx.getSource(), ctx.getArgument("key", TeamProperty.class), string(ctx, "value")))
								)
								.executes(ctx -> team(ctx).settings(ctx.getSource(), ctx.getArgument("key", TeamProperty.class), ""))
						)
				)
				.then(Commands.literal("settings_for")
						.requires(requiresOPorSP())
						.then(Commands.argument("team", FTBTeamsAPI.argument())
								.then(Commands.argument("key", new TeamPropertyArgument())
										.then(Commands.argument("value", StringArgumentType.greedyString())
												.executes(ctx -> team(ctx, "team").settings(ctx.getSource(), ctx.getArgument("key", TeamProperty.class), string(ctx, "value")))
										)
										.executes(ctx -> team(ctx, "team").settings(ctx.getSource(), ctx.getArgument("key", TeamProperty.class), ""))
								)
						)
				)
				.then(Commands.literal("join")
						.then(Commands.argument("team", FTBTeamsAPI.argument())
								.executes(ctx -> team(ctx, "team").join(ctx.getSource()))
						)
				)
				.then(Commands.literal("leave")
						.executes(ctx -> team(ctx).leave(ctx.getSource()))
				)
				.then(Commands.literal("invite")
						.then(Commands.argument("players", GameProfileArgument.gameProfile())
								.executes(ctx -> teamAsOwner(ctx).invite(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "players")))
						)
				)
				.then(Commands.literal("deny_invite")
						.then(Commands.argument("team", FTBTeamsAPI.argument())
								.executes(ctx -> team(ctx, "team").denyInvite(ctx.getSource()))
						)
				)
				.then(Commands.literal("kick")
						.then(Commands.argument("players", GameProfileArgument.gameProfile())
								.executes(ctx -> teamAsOwner(ctx).kick(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "players")))
						)
				)
				.then(Commands.literal("transfer_ownership")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(ctx -> teamAsOwner(ctx).transferOwnership(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
						)
				)
				.then(Commands.literal("info")
						.then(Commands.argument("team", FTBTeamsAPI.argument())
								.executes(ctx -> team(ctx, "team").info(ctx.getSource()))
						)
						.executes(ctx -> team(ctx).info(ctx.getSource()))
				)
				.then(Commands.literal("list")
						.executes(ctx -> list(ctx.getSource()))
				)
				.then(Commands.literal("msg")
						.then(Commands.argument("message", StringArgumentType.greedyString())
								.executes(ctx -> team(ctx).msg(ctx.getSource().getPlayerOrException(), string(ctx, "message")))
						)
				)
				.executes(ctx -> team(ctx).gui(ctx.getSource()))
		);
		 */
	}

	private int list(CommandSourceStack source) {
		TextComponent list = new TextComponent("");

		boolean first = true;

		for (Team team : FTBTeamsAPI.getManager().getTeams()) {
			if (first) {
				first = false;
			} else {
				list.append(", ");
			}

			list.append(team.getName());
		}

		source.sendSuccess(new TranslatableComponent("ftbteams.list", list), true);
		return Command.SINGLE_SUCCESS;
	}
}