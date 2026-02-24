package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.function.Predicate;

public class ListCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("list")
                .executes(ctx -> list(ctx.getSource(), t -> true))
                .then(Commands.literal("parties")
                        .executes(ctx -> list(ctx.getSource(), Team::isPartyTeam))
                )
                .then(Commands.literal("server_teams")
                        .executes(ctx -> list(ctx.getSource(), Team::isServerTeam))
                )
                .then(Commands.literal("players")
                        .executes(ctx -> list(ctx.getSource(), Team::isPlayerTeam))
                );
    }

    private static int list(CommandSourceStack source, Predicate<Team> predicate) {
        Component teams = FTBTeamsAPI.api().getManager().getTeams().stream()
                .filter(predicate)
                .sorted(Comparator.comparing(Team::getShortName))
                .map(Team::getName)
                .reduce((c1, c2) -> c1.copy().append(", ").append(c2))
                .orElse(Component.literal("<none>"));

        source.sendSuccess(() -> Component.translatable("ftbteams.list", teams), false);
        return Command.SINGLE_SUCCESS;
    }
}
