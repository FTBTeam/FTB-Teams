package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author LatvianModder
 */
public class TeamArgument implements ArgumentType<TeamArgumentProvider> {
	public static final SimpleCommandExceptionType ALREADY_IN_PARTY = new SimpleCommandExceptionType(new TranslatableComponent("ftbteams.already_in_party"));
	public static final SimpleCommandExceptionType NOT_IN_PARTY = new SimpleCommandExceptionType(new TranslatableComponent("ftbteams.not_in_party"));
	public static final DynamicCommandExceptionType TEAM_NOT_FOUND = new DynamicCommandExceptionType(object -> new TranslatableComponent("ftbteams.team_not_found", object));
	public static final DynamicCommandExceptionType CANT_EDIT = new DynamicCommandExceptionType(object -> new TranslatableComponent("ftbteams.cant_edit", object));
	public static final Dynamic2CommandExceptionType NOT_MEMBER = new Dynamic2CommandExceptionType((a, b) -> new TranslatableComponent("ftbteams.not_member", a, b));
	public static final DynamicCommandExceptionType NOT_INVITED = new DynamicCommandExceptionType(object -> new TranslatableComponent("ftbteams.not_invited", object));

	public static TeamArgument create() {
		return new TeamArgument();
	}

	public static Team get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		return context.getArgument(name, TeamArgumentProvider.class).getTeam(context.getSource());
	}

	private TeamArgument() {
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

		private CommandSyntaxException error() {
			return TeamArgument.TEAM_NOT_FOUND.create(id);
		}

		@Override
		public Team getTeam(CommandSourceStack source) throws CommandSyntaxException {
			Team team = FTBTeamsAPI.getManager().getTeamNameMap().get(id);

			if (team != null) {
				return team;
			}

			GameProfile profile = source.getServer().getProfileCache().get(id);

			if (profile != null) {
				Team t = FTBTeamsAPI.getManager().getPlayerTeam(profile.getId());

				if (t != null) {
					return t;
				}
			}

			throw error();
		}
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
		List<String> list = new ArrayList<>();

		if (FTBTeamsAPI.isManagerLoaded()) {
			for (Map.Entry<String, Team> entry : FTBTeamsAPI.getManager().getTeamNameMap().entrySet()) {
				if (entry.getValue().getType().isPlayer() && !((PlayerTeam) entry.getValue()).playerName.isEmpty()) {
					list.add(((PlayerTeam) entry.getValue()).playerName);
				} else {
					list.add(entry.getKey());
				}
			}
		}

		return SharedSuggestionProvider.suggest(list, builder);
	}
}