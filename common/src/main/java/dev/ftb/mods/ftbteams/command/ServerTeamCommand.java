package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyArgument;
import dev.ftb.mods.ftbteams.data.TeamType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static dev.ftb.mods.ftbteams.command.FTBTeamsCommands.*;

public class ServerTeamCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("server")
                .requires(requiresOPorSP())
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ServerTeamCommand::tryCreateServerTeam)
                        )
                )
                .then(Commands.literal("delete")
                        .then(createTeamArg(TeamType.SERVER)
                                .executes(ctx -> serverTeamArg(ctx).delete(ctx.getSource()))
                        )
                )
                .then(Commands.literal("settings")
                        .then(createTeamArg(TeamType.SERVER)
                                .then(Commands.argument("key", TeamPropertyArgument.create())
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> serverTeamArg(ctx).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), string(ctx, "value")))
                                        )
                                        .executes(ctx -> serverTeamArg(ctx).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), ""))
                                )
                        )
                );
    }

    private static int tryCreateServerTeam(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FTBTeamsAPI.api().getManager().createServerTeam(ctx.getSource(), string(ctx, "name"), null, null);
        return Command.SINGLE_SUCCESS;
    }
}
