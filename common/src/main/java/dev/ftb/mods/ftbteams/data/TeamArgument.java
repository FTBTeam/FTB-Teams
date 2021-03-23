package dev.ftb.mods.ftbteams.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class TeamArgument implements ArgumentType<TeamArgumentProvider> {
	public static final SimpleCommandExceptionType ALREADY_IN_TEAM = new SimpleCommandExceptionType(new TranslatableComponent("ftbteams.already_in_team"));
	public static final SimpleCommandExceptionType NOT_IN_PARTY = new SimpleCommandExceptionType(new TranslatableComponent("ftbteams.not_in_party"));
	public static final DynamicCommandExceptionType TEAM_NOT_FOUND = new DynamicCommandExceptionType(object -> new TranslatableComponent("ftbteams.team_not_found", object));
	public static final DynamicCommandExceptionType NOT_OWNER = new DynamicCommandExceptionType(object -> new TranslatableComponent("ftbteams.not_owner", object));
	public static final Dynamic2CommandExceptionType NOT_MEMBER = new Dynamic2CommandExceptionType((a, b) -> new TranslatableComponent("ftbteams.not_member", a, b));
	public static final DynamicCommandExceptionType NOT_INVITED = new DynamicCommandExceptionType(object -> new TranslatableComponent("ftbteams.not_invited", object));

	public static Team get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		return context.getArgument(name, TeamArgumentProvider.class).getTeam(context.getSource());
	}

	private static class SelectorProvider implements TeamArgumentProvider {
		private final EntitySelector selector;

		private SelectorProvider(EntitySelector s) {
			selector = s;
		}

		@Override
		public Team getTeam(CommandSourceStack source) throws CommandSyntaxException {
			return FTBTeamsAPI.getManager().getPlayerTeam(selector.findSinglePlayer(source));
		}
	}

	private static class IDProvider implements TeamArgumentProvider {
		private final String id;

		private IDProvider(String s) {
			id = s;
		}

		private static boolean isNumber(String s) {
			for (int i = 0; i < s.length(); i++) {
				if (s.charAt(i) < '0' || s.charAt(i) > '9') {
					return false;
				}
			}

			return true;
		}

		private CommandSyntaxException error() {
			return TeamArgument.TEAM_NOT_FOUND.create(id);
		}

		@Override
		public Team getTeam(CommandSourceStack source) throws CommandSyntaxException {
			/*
			int i = id.lastIndexOf('#');

			if (i != -1) {
				try {
					int iid = Integer.parseInt(id.substring(i + 1));
					return Optional.ofNullable(FTBTeamsAPI.getManager().getTeamByID(iid)).orElseThrow(this::error);
				} catch (Exception ex) {
					throw error();
				}
			} else if (isNumber(id)) {
				int iid = Integer.parseInt(id);
				return Optional.ofNullable(FTBTeamsAPI.getManager().getTeamByID(iid)).orElseThrow(this::error);
			}

			GameProfile profile = source.getServer().getProfileCache().get(id);

			if (profile != null) {
				return Optional.ofNullable(FTBTeamsAPI.getManager().getTeamByID(profile)).orElseThrow(this::error);
			}

			 */
			throw error();
		}
	}

	public final Supplier<Iterable<String>> suggestions;

	public TeamArgument(Supplier<Iterable<String>> s) {
		suggestions = s;
	}

	@Override
	public TeamArgumentProvider parse(StringReader reader) throws CommandSyntaxException {
		if (reader.canRead() && reader.peek() == '@') {
			EntitySelector selector = new EntitySelectorParser(reader).parse();

			if (selector.includesEntities()) {
				throw EntityArgument.ERROR_ONLY_PLAYERS_ALLOWED.create();
			} else {
				return new SelectorProvider(selector);
			}
		}

		int i = reader.getCursor();

		while (reader.canRead() && reader.peek() != ' ') {
			reader.skip();
		}

		return new IDProvider(reader.getString().substring(i, reader.getCursor()));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(suggestions.get(), builder);
	}
}