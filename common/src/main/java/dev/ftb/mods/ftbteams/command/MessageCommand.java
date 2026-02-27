package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class MessageCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("msg")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            FTBTeamsCommands.getTeam(ctx).sendMessage(ctx.getSource().getPlayerOrException().getUUID(), StringArgumentType.getString(ctx, "text"));
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }
}
