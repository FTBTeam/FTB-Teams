package com.feed_the_beast.mods.ftbteams.api;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * @author LatvianModder
 */
public interface TeamArgument extends ArgumentType<TeamArgumentProvider>
{
	SimpleCommandExceptionType NO_GUI_LIBRARY = new SimpleCommandExceptionType(new TranslatableComponent("ftbteams.no_gui_library"));
	SimpleCommandExceptionType ALREADY_IN_TEAM = new SimpleCommandExceptionType(new TranslatableComponent("ftbteams.already_in_team"));
	SimpleCommandExceptionType NOT_IN_TEAM = new SimpleCommandExceptionType(new TranslatableComponent("ftbteams.not_in_team"));
	DynamicCommandExceptionType TEAM_NOT_FOUND = new DynamicCommandExceptionType(object -> new TranslatableComponent("ftbteams.team_not_found", object));
	DynamicCommandExceptionType NOT_OWNER = new DynamicCommandExceptionType(object -> new TranslatableComponent("ftbteams.not_owner", object));
	Dynamic2CommandExceptionType NOT_MEMBER = new Dynamic2CommandExceptionType((a, b) -> new TranslatableComponent("ftbteams.not_member", a, b));
	DynamicCommandExceptionType NOT_INVITED = new DynamicCommandExceptionType(object -> new TranslatableComponent("ftbteams.not_invited", object));

	static Team get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
	{
		return context.getArgument(name, TeamArgumentProvider.class).getTeam(context.getSource());
	}
}