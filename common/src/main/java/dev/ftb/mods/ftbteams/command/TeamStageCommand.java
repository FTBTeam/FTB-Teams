package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamStagesHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

import static dev.ftb.mods.ftbteams.command.FTBTeamsCommands.requiresOPorSP;

public class TeamStageCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("teamstage")
                .requires(requiresOPorSP())
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("add")
                                .then(Commands.argument("stage", StringArgumentType.string())
                                        .executes(ctx -> updateTeamStage(ctx,
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "stage"),
                                                true)
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("stage", StringArgumentType.string())
                                        .executes(ctx -> updateTeamStage(ctx,
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "stage"),
                                                false)
                                        )
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(ctx -> listTeamStages(ctx, EntityArgument.getPlayer(ctx, "player")))
                        )
                );
    }

    private static int updateTeamStage(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String stage, boolean adding) throws CommandSyntaxException {
        Team team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(player.getDisplayName()));

        if (adding) {
            if (TeamStagesHelper.addTeamStage(team, stage)) {
                ctx.getSource().sendSuccess(() -> Component.translatable("ftbteams.message.added_stage", stage), false);
            }
        } else if (TeamStagesHelper.removeTeamStage(team, stage)) {
            ctx.getSource().sendSuccess(() -> Component.translatable("ftbteams.message.removed_stage", stage), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int listTeamStages(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        Team team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(player.getDisplayName()));

        Collection<String> stages = TeamStagesHelper.getStages(team);
        if (stages.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("ftbteams.message.no_team_stages").withStyle(ChatFormatting.GOLD), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable("ftbteams.message.team_stages_header", stages.size()).withStyle(ChatFormatting.YELLOW), false);
            stages.forEach(stage -> ctx.getSource().sendSuccess(() -> Component.literal("• ").append(stage), false));
        }
        return 0;
    }
}
