package com.feed_the_beast.mods.ftbteams.impl;

import com.feed_the_beast.mods.ftbteams.api.FTBTeamsAPI;
import com.feed_the_beast.mods.ftbteams.api.Team;
import com.feed_the_beast.mods.ftbteams.api.TeamArgument;
import com.feed_the_beast.mods.ftbteams.api.TeamProperty;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class FTBTeamsCommands
{
	private Predicate<CommandSource> requiresOPorSP()
	{
		return source -> source.getServer().isSinglePlayer() || source.hasPermissionLevel(2);
	}

	private String string(CommandContext<?> context, String name)
	{
		return StringArgumentType.getString(context, name);
	}

	private TeamImpl team(CommandContext<CommandSource> context, String arg) throws CommandSyntaxException
	{
		return (TeamImpl) TeamArgument.get(context, arg);
	}

	private TeamImpl team(CommandContext<CommandSource> context) throws CommandSyntaxException
	{
		ServerPlayerEntity player = context.getSource().asPlayer();
		Optional<Team> team = FTBTeamsAPI.INSTANCE.getManager().getTeam(player);

		if (!team.isPresent())
		{
			throw TeamArgument.NOT_IN_TEAM.create();
		}

		return (TeamImpl) team.get();
	}

	private TeamImpl teamAsOwner(CommandContext<CommandSource> context) throws CommandSyntaxException
	{
		ServerPlayerEntity player = context.getSource().asPlayer();
		Optional<Team> team = FTBTeamsAPI.INSTANCE.getManager().getTeam(player);

		if (!team.isPresent())
		{
			throw TeamArgument.NOT_IN_TEAM.create();
		}

		Team t = team.get();

		if (!t.isOwner(player))
		{
			throw TeamArgument.NOT_OWNER.create(t.getName());
		}

		return (TeamImpl) t;
	}

	public void register(CommandDispatcher<CommandSource> dispatcher)
	{
		LiteralCommandNode<CommandSource> command = dispatcher.register(Commands.literal("ftbteams")
				.then(Commands.literal("create")
						.then(Commands.argument("name", StringArgumentType.greedyString())
								.executes(ctx -> create(ctx.getSource(), string(ctx, "name")))
						)
						.executes(ctx -> create(ctx.getSource(), ""))
				)
				.then(Commands.literal("create_server_team")
						.requires(requiresOPorSP())
						.then(Commands.argument("name", StringArgumentType.greedyString())
								.executes(ctx -> createServer(ctx.getSource(), string(ctx, "name")))
						)
				)
				.then(Commands.literal("delete")
						.then(Commands.argument("team", FTBTeamsAPI.INSTANCE.argument())
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
						.then(Commands.argument("team", FTBTeamsAPI.INSTANCE.argument())
								.then(Commands.argument("key", new TeamPropertyArgument())
										.then(Commands.argument("value", StringArgumentType.greedyString())
												.executes(ctx -> team(ctx, "team").settings(ctx.getSource(), ctx.getArgument("key", TeamProperty.class), string(ctx, "value")))
										)
										.executes(ctx -> team(ctx, "team").settings(ctx.getSource(), ctx.getArgument("key", TeamProperty.class), ""))
								)
						)
				)
				.then(Commands.literal("join")
						.then(Commands.argument("team", FTBTeamsAPI.INSTANCE.argument())
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
						.then(Commands.argument("team", FTBTeamsAPI.INSTANCE.argument())
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
						.then(Commands.argument("team", FTBTeamsAPI.INSTANCE.argument())
								.executes(ctx -> team(ctx, "team").info(ctx.getSource()))
						)
						.executes(ctx -> team(ctx).info(ctx.getSource()))
				)
				.then(Commands.literal("list")
						.executes(ctx -> list(ctx.getSource()))
				)
				.then(Commands.literal("msg")
						.then(Commands.argument("message", StringArgumentType.greedyString())
								.executes(ctx -> team(ctx).msg(ctx.getSource(), string(ctx, "message")))
						)
				)
				.executes(ctx -> team(ctx).gui(ctx.getSource()))
		);

		//dispatcher.register(Commands.literal("ftbteam").redirect(command));
	}

	private static int create(CommandSource source, String name) throws CommandSyntaxException
	{
		Team team = TeamManagerImpl.instance.createPlayerTeam(source.asPlayer(), name);
		source.sendFeedback(new StringTextComponent("Created new team ").appendSibling(team.getName()), true);
		return Command.SINGLE_SUCCESS;
	}

	private int createServer(CommandSource source, String name) throws CommandSyntaxException
	{
		TeamManagerImpl manager = TeamManagerImpl.instance;
		TeamImpl team = manager.newTeam();
		team.serverTeam = true;
		team.setProperty(TeamImpl.DISPLAY_NAME, name);
		team.setProperty(TeamImpl.COLOR, TeamImpl.randomColor(source.getWorld().rand));
		team.create();

		source.sendFeedback(new StringTextComponent("Created new server team ").appendSibling(team.getName()), true);
		return Command.SINGLE_SUCCESS;
	}

	private int list(CommandSource source)
	{
		ITextComponent list = new StringTextComponent("");

		boolean first = true;

		for (Team team : FTBTeamsAPI.INSTANCE.getManager().getTeams())
		{
			if (first)
			{
				first = false;
			}
			else
			{
				list.appendText(", ");
			}

			list.appendSibling(team.getName());
		}

		source.sendFeedback(new TranslationTextComponent("ftbteams.list", list), true);
		return Command.SINGLE_SUCCESS;
	}
}