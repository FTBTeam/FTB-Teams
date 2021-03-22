package com.feed_the_beast.mods.ftbteams;

import com.feed_the_beast.mods.ftbteams.client.FTBTeamsClient;
import com.feed_the_beast.mods.ftbteams.data.FTBTeamsCommands;
import com.feed_the_beast.mods.ftbteams.data.Team;
import com.feed_the_beast.mods.ftbteams.data.TeamArgument;
import com.feed_the_beast.mods.ftbteams.data.TeamManager;
import com.feed_the_beast.mods.ftbteams.data.TeamPropertyArgument;
import com.feed_the_beast.mods.ftbteams.event.TeamConfigEvent;
import com.feed_the_beast.mods.ftbteams.net.FTBTeamsNet;
import com.mojang.brigadier.CommandDispatcher;
import me.shedaniel.architectury.event.events.CommandRegistrationEvent;
import me.shedaniel.architectury.event.events.LifecycleEvent;
import me.shedaniel.architectury.event.events.PlayerEvent;
import me.shedaniel.architectury.utils.EnvExecutor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class FTBTeams {
	public static final String MOD_ID = "ftbteams";
	public static final String MOD_NAME = "FTB Teams";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
	public static FTBTeamsCommon PROXY;
	private static final List<String> teamArgumentSuggestions = new ArrayList<>();

	public FTBTeams() {
		LifecycleEvent.SERVER_BEFORE_START.register(this::serverAboutToStart);
		CommandRegistrationEvent.EVENT.register(this::registerCommands);
		LifecycleEvent.SERVER_STOPPED.register(this::serverStopped);
		LifecycleEvent.SERVER_WORLD_SAVE.register(this::worldSaved);
		TeamConfigEvent.EVENT.register(this::teamConfig);
		PlayerEvent.PLAYER_JOIN.register(this::playerLoggedIn);
		FTBTeamsNet.init();
		PROXY = EnvExecutor.getEnvSpecific(() -> FTBTeamsClient::new, () -> FTBTeamsCommon::new);
	}

	public void setup() {
		ArgumentTypes.register("ftbteams_team", TeamArgument.class, new EmptyArgumentSerializer<>(() -> new TeamArgument(() -> teamArgumentSuggestions)));
		ArgumentTypes.register("ftbteams_team_property", TeamPropertyArgument.class, new EmptyArgumentSerializer<>(TeamPropertyArgument::new));
	}

	private void serverAboutToStart(MinecraftServer server) {
		TeamManager.INSTANCE = new TeamManager(server);
		TeamManager.INSTANCE.load();
	}

	private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection selection) {
		new FTBTeamsCommands().register(dispatcher);
	}

	private void serverStopped(MinecraftServer server) {
		TeamManager.INSTANCE = null;
	}

	private void worldSaved(ServerLevel level) {
		if (TeamManager.INSTANCE != null) {
			TeamManager.INSTANCE.saveNow();
		}
	}

	private void teamConfig(TeamConfigEvent event) {
		event.add(Team.DISPLAY_NAME);
		event.add(Team.DESCRIPTION);
		event.add(Team.COLOR);
		event.add(Team.FREE_TO_JOIN);
	}

	private void playerLoggedIn(ServerPlayer player) {
		TeamManager.INSTANCE.playerLoggedIn(player);
	}
}