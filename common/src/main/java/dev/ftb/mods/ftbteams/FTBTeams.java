package dev.ftb.mods.ftbteams;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.ChatEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.ftb.mods.ftblibrary.config.manager.ConfigManager;
import dev.ftb.mods.ftblibrary.nbtedit.NBTEditResponseHandlers;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.event.TeamCollectPropertiesEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamManagerEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.client.FTBTeamsClient;
import dev.ftb.mods.ftbteams.config.ServerConfig;
import dev.ftb.mods.ftbteams.data.AbstractTeam;
import dev.ftb.mods.ftbteams.data.FTBTeamsCommands;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import dev.ftb.mods.ftbteams.net.FTBTeamsNet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class FTBTeams {
	public static final Logger LOGGER = LogManager.getLogger(FTBTeamsAPI.MOD_NAME);

	public FTBTeams() {
		FTBTeamsAPI._init(FTBTeamsAPIImpl.INSTANCE);

		LifecycleEvent.SERVER_BEFORE_START.register(this::serverAboutToStart);
		LifecycleEvent.SERVER_STARTED.register(this::serverStarted);
		CommandRegistrationEvent.EVENT.register(this::registerCommands);
		LifecycleEvent.SERVER_STOPPED.register(this::serverStopped);
		LifecycleEvent.SERVER_LEVEL_SAVE.register(this::worldSaved);
		TeamEvent.COLLECT_PROPERTIES.register(this::teamConfig);
		PlayerEvent.PLAYER_JOIN.register(this::playerLoggedIn);
		PlayerEvent.PLAYER_QUIT.register(this::playerLoggedOut);
		PlayerEvent.PLAYER_CLONE.register(this::playerCloned);
		ChatEvent.RECEIVED.register(this::chatReceived);

		EnvExecutor.runInEnv(Env.CLIENT, () -> FTBTeamsClient::init);

		ConfigManager.getInstance().registerServerConfig(ServerConfig.CONFIG, FTBTeamsAPI.MOD_ID + ".config.server", true);

		FTBTeamsNet.register();
	}

	private void serverStarted(MinecraftServer server) {
		NBTEditResponseHandlers.INSTANCE.registerHandler("ftbteams:team", (serverPlayer, info, data) -> {
			TeamManagerImpl.INSTANCE.getTeamByID(info.getUUID("id")).ifPresent(team -> {
				if (team instanceof AbstractTeam abstractTeam) {
					abstractTeam.deserializeNBT(data, server.registryAccess());
					abstractTeam.markDirty();
				}
			});
		});
	}

	private void serverAboutToStart(MinecraftServer server) {
		TeamManagerImpl.INSTANCE = new TeamManagerImpl(server);
		TeamManagerEvent.CREATED.invoker().accept(new TeamManagerEvent(TeamManagerImpl.INSTANCE));
		TeamManagerImpl.INSTANCE.load();
	}

	private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection selection) {
		new FTBTeamsCommands().register(dispatcher);
	}

	private void serverStopped(MinecraftServer server) {
		TeamManagerEvent.DESTROYED.invoker().accept(new TeamManagerEvent(TeamManagerImpl.INSTANCE));
		TeamManagerImpl.INSTANCE = null;
	}

	private void worldSaved(ServerLevel level) {
		if (TeamManagerImpl.INSTANCE != null) {
			TeamManagerImpl.INSTANCE.saveNow();
		}
	}

	private void teamConfig(TeamCollectPropertiesEvent event) {
		event.add(TeamProperties.DISPLAY_NAME);
		event.add(TeamProperties.DESCRIPTION);
		event.add(TeamProperties.COLOR);
		event.add(TeamProperties.FREE_TO_JOIN);
		event.add(TeamProperties.MAX_MSG_HISTORY_SIZE);
		event.add(TeamProperties.TEAM_STAGES);
		event.add(TeamProperties.LIVES_REMAINING);
	}

	private void playerLoggedIn(ServerPlayer player) {
		if (TeamManagerImpl.INSTANCE != null) {
			TeamManagerImpl.INSTANCE.playerLoggedIn(player, player.getUUID(), player.getScoreboardName());
		}
	}

	private void playerLoggedOut(ServerPlayer player) {
		if (TeamManagerImpl.INSTANCE != null) {
			TeamManagerImpl.INSTANCE.playerLoggedOut(player);
		}
	}

	private void playerCloned(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wonGame) {
		if (!wonGame) {
			ServerConfig.limitedLives().ifPresent(maxLives -> FTBTeamsAPI.api().getManager().getTeamForPlayer(newPlayer).ifPresent(team -> {
				if (team instanceof PartyTeam partyTeam) {
					MinecraftServer server = newPlayer.getServer();
					// defer a tick so player is alive again and gets client team syncs
					if (server != null) server.tell(new TickTask(server.getTickCount(), () -> {
						int newLives = partyTeam.getProperty(TeamProperties.LIVES_REMAINING) - 1;
						if (newLives >= 0) {
							partyTeam.setProperty(TeamProperties.LIVES_REMAINING, newLives);
							partyTeam.syncOnePropertyToTeam(TeamProperties.LIVES_REMAINING, newLives);
							partyTeam.sendMessage(Util.NIL_UUID, Component.translatable("ftbteams.lost_a_life", newLives, maxLives).withStyle(ChatFormatting.RED));
						} else {
							kickPlayerNoLivesLeft(newPlayer, partyTeam);
						}
					}));
				}
			}));
		}
	}

	private static void kickPlayerNoLivesLeft(ServerPlayer player, PartyTeam partyTeam) {
		try {
			if (partyTeam.getOnlineMembers().size() > 1) {
				partyTeam.sendMessage(Util.NIL_UUID, Component.translatable("ftbteams.kicked_no_lives", player.getName()).withStyle(ChatFormatting.RED));
			} else {
				partyTeam.sendMessage(Util.NIL_UUID, Component.translatable("ftbteams.disbanded_no_lives").withStyle(ChatFormatting.RED));
			}
			partyTeam.kickPlayerForcibly(player);
		} catch (CommandSyntaxException e) {
			FTBTeams.LOGGER.error("can't kick player {} from 0-lives team {}: {}", player.getUUID(), partyTeam.getTeamId(), e.getMessage());
		}
	}

	private EventResult chatReceived(@Nullable ServerPlayer player, Component component) {
		if (TeamManagerImpl.INSTANCE != null && player != null && TeamManagerImpl.INSTANCE.isChatRedirected(player)) {
			return FTBTeamsAPI.api().getManager().getTeamForPlayer(player).map(team -> {
				team.sendMessage(player.getUUID(), component);
				return EventResult.interruptFalse();
			}).orElse(EventResult.pass());
		}
		return EventResult.pass();
	}
}
