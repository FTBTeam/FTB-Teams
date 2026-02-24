package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.TeamType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;

import static dev.ftb.mods.ftbteams.command.FTBTeamsCommands.*;

public class ForceCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("force")
                .requires(requiresOPorSP())
                .then(Commands.literal("disband")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(createTeamArg(TeamType.PARTY)
                                .executes(ctx -> partyTeamArg(ctx, TeamRank.NONE).forceDisband(ctx.getSource()))
                        )
                )
                .then(Commands.literal("add")
                        .then(createTeamArg(TeamType.PARTY)
                                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                        .executes(ForceCommand::forceAddPlayers))
                        )
                )
                .then(Commands.literal("remove")
                        .then(createTeamArg(TeamType.PARTY)
                                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                        .executes(ForceCommand::forceRemovePlayers))
                        )
                );
    }

    private static int forceAddPlayers(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int res = 0;
        PartyTeam party = partyTeamArg(ctx, TeamRank.NONE);
        for (NameAndId profile : GameProfileArgument.getGameProfiles(ctx, "players")) {
            res += party.join(null, profile);
        }
        return res;
    }

    private static int forceRemovePlayers(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return partyTeamArg(ctx, TeamRank.NONE).kick(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "players"));
    }
}
