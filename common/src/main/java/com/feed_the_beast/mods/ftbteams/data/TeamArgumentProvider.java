package com.feed_the_beast.mods.ftbteams.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface TeamArgumentProvider {
	Team getTeam(CommandSourceStack source) throws CommandSyntaxException;
}