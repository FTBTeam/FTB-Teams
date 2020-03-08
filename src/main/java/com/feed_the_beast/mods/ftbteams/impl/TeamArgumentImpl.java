package com.feed_the_beast.mods.ftbteams.impl;

import com.feed_the_beast.mods.ftbteams.api.FTBTeamsAPI;
import com.feed_the_beast.mods.ftbteams.api.Team;
import com.feed_the_beast.mods.ftbteams.api.TeamArgument;
import com.feed_the_beast.mods.ftbteams.api.TeamArgumentProvider;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.command.arguments.EntitySelectorParser;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class TeamArgumentImpl implements TeamArgument
{
	private static class SelectorProvider implements TeamArgumentProvider
	{
		private final EntitySelector selector;

		private SelectorProvider(EntitySelector s)
		{
			selector = s;
		}

		@Override
		public Team getTeam(CommandSource source) throws CommandSyntaxException
		{
			return FTBTeamsAPI.INSTANCE.getManager().getTeam(selector.selectOnePlayer(source)).orElseThrow(TeamArgument.NOT_IN_TEAM::create);
		}
	}

	private static class IDProvider implements TeamArgumentProvider
	{
		private final String id;

		private IDProvider(String s)
		{
			id = s;
		}

		@Override
		public Team getTeam(CommandSource source) throws CommandSyntaxException
		{
			try
			{
				Team team = FTBTeamsAPI.INSTANCE.getManager().getTeamNameMap().get(id);

				if (team != null)
				{
					return team;
				}
			}
			catch (Exception ex)
			{
			}

			GameProfile profile = source.getServer().getPlayerProfileCache().getGameProfileForUsername(id);

			if (profile != null)
			{
				return FTBTeamsAPI.INSTANCE.getManager().getTeam(profile).orElseThrow(TeamArgument.NOT_IN_TEAM::create);
			}

			throw TeamArgument.TEAM_NOT_FOUND.create(id);
		}
	}

	public final Supplier<Iterable<String>> suggestions;

	public TeamArgumentImpl(Supplier<Iterable<String>> s)
	{
		suggestions = s;
	}

	@Override
	public TeamArgumentProvider parse(StringReader reader) throws CommandSyntaxException
	{
		if (reader.canRead() && reader.peek() == '@')
		{
			EntitySelector selector = new EntitySelectorParser(reader).parse();

			if (selector.includesEntities())
			{
				throw EntityArgument.ONLY_PLAYERS_ALLOWED.create();
			}
			else
			{
				return new SelectorProvider(selector);
			}
		}

		int i = reader.getCursor();

		while (reader.canRead() && reader.peek() != ' ')
		{
			reader.skip();
		}

		return new IDProvider(reader.getString().substring(i, reader.getCursor()));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
	{
		return ISuggestionProvider.suggest(suggestions.get(), builder);
	}
}