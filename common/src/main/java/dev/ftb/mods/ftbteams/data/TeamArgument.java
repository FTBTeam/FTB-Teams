package dev.ftb.mods.ftbteams.data;

import com.google.gson.JsonObject;
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
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashSet;
import java.util.concurrent.CompletableFuture;

/**
 * @author LatvianModder
 */
public class TeamArgument implements ArgumentType<TeamArgumentProvider> {
	public static final SimpleCommandExceptionType ALREADY_IN_PARTY = new SimpleCommandExceptionType(Component.translatable("ftbteams.already_in_party"));
	public static final DynamicCommandExceptionType PLAYER_IN_PARTY = new DynamicCommandExceptionType(object -> Component.translatable("ftbteams.player_already_in_party", object));
	public static final SimpleCommandExceptionType NOT_IN_PARTY = new SimpleCommandExceptionType(Component.translatable("ftbteams.not_in_party"));
	public static final DynamicCommandExceptionType TEAM_NOT_FOUND = new DynamicCommandExceptionType(object -> Component.translatable("ftbteams.team_not_found", object));
	public static final DynamicCommandExceptionType CANT_EDIT = new DynamicCommandExceptionType(object -> Component.translatable("ftbteams.cant_edit", object));
	public static final Dynamic2CommandExceptionType NOT_MEMBER = new Dynamic2CommandExceptionType((a, b) -> Component.translatable("ftbteams.not_member", a, b));
	public static final Dynamic2CommandExceptionType NOT_OFFICER = new Dynamic2CommandExceptionType((a, b) -> Component.translatable("ftbteams.not_officer", a, b));
	public static final DynamicCommandExceptionType NOT_INVITED = new DynamicCommandExceptionType(object -> Component.translatable("ftbteams.not_invited", object));
	public static final SimpleCommandExceptionType OWNER_CANT_LEAVE = new SimpleCommandExceptionType(Component.translatable("ftbteams.owner_cant_leave"));
	public static final SimpleCommandExceptionType CANT_KICK_OWNER = new SimpleCommandExceptionType(Component.translatable("ftbteams.cant_kick_owner"));
	public static final SimpleCommandExceptionType API_OVERRIDE = new SimpleCommandExceptionType(Component.translatable("ftbteams.api_override"));

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

			return source.getServer().getProfileCache().get(id)
					.map(GameProfile::getId)
					.map(FTBTeamsAPI.getManager()::getPlayerTeam)
					.orElseThrow(this::error);
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
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder builder) {
		if (commandContext.getSource() instanceof SharedSuggestionProvider) {
			LinkedHashSet<String> list = new LinkedHashSet<>();

			if (commandContext.getSource() instanceof CommandSourceStack) {
				if (FTBTeamsAPI.isManagerLoaded()) {
					for (Team team : FTBTeamsAPI.getManager().getTeams()) {
						if (!team.getType().isPlayer()) {
							list.add(team.getStringID());
						}
					}
				}
			} else if (ClientTeamManager.INSTANCE != null && !ClientTeamManager.INSTANCE.invalid) {
				for (ClientTeam team : ClientTeamManager.INSTANCE.teamMap.values()) {
					if (!team.getType().isPlayer()) {
						list.add(team.getStringID());
					}
				}
			}

			list.addAll(((SharedSuggestionProvider) commandContext.getSource()).getOnlinePlayerNames());
			return SharedSuggestionProvider.suggest(list, builder);
		}

		return Suggestions.empty();
	}

	public static class Info implements ArgumentTypeInfo<TeamArgument, Info.Template> {
		@Override
		public void serializeToNetwork(Template template, FriendlyByteBuf friendlyByteBuf) {

		}

		@Override
		public Template deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
			return new Template();
		}

		@Override
		public void serializeToJson(Template template, JsonObject jsonObject) {

		}

		@Override
		public Template unpack(TeamArgument argumentType) {
			return new Template();
		}

		public final class Template implements ArgumentTypeInfo.Template<TeamArgument> {
			@Override
			public TeamArgument instantiate(CommandBuildContext commandBuildContext) {
				return TeamArgument.create();
			}

			@Override
			public ArgumentTypeInfo<TeamArgument, ?> type() {
				return Info.this;
			}
		}
	}
}
