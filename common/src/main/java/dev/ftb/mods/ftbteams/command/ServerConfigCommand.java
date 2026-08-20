package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftblibrary.net.EditConfigChoicePacket;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import dev.ftb.mods.ftbteams.config.ServerConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static dev.ftb.mods.ftbteams.command.FTBTeamsCommands.requiresOPorSP;

public class ServerConfigCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("serverconfig")
                .requires(requiresOPorSP())
                .executes(context -> {
                    Server2PlayNetworking.send(context.getSource().getPlayerOrException(), EditConfigChoicePacket.server(ServerConfig.KEY));
                    return Command.SINGLE_SUCCESS;
                });
    }
}
