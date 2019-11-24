package com.feed_the_beast.mods.ftbteams.api;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface TeamArgumentProvider
{
	Team getTeam(CommandSource source) throws CommandSyntaxException;
}