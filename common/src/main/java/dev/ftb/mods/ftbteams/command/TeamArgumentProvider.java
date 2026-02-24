package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.commands.CommandSourceStack;

@FunctionalInterface
public interface TeamArgumentProvider {
	Team getTeam(CommandSourceStack source) throws CommandSyntaxException;
}