package com.feed_the_beast.mods.ftbteams;

import com.feed_the_beast.mods.ftbteams.api.FTBTeamsAPI;
import com.feed_the_beast.mods.ftbteams.event.TeamConfigEvent;
import com.feed_the_beast.mods.ftbteams.impl.FTBTeamsAPIImpl;
import com.feed_the_beast.mods.ftbteams.impl.FTBTeamsCommands;
import com.feed_the_beast.mods.ftbteams.impl.TeamArgumentImpl;
import com.feed_the_beast.mods.ftbteams.impl.TeamImpl;
import com.feed_the_beast.mods.ftbteams.impl.TeamManagerImpl;
import com.feed_the_beast.mods.ftbteams.impl.TeamPropertyArgument;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import me.shedaniel.architectury.event.events.CommandRegistrationEvent;
import me.shedaniel.architectury.event.events.LifecycleEvent;
import me.shedaniel.architectury.event.events.PlayerEvent;
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

public class FTBTeams
{
	public static final String MOD_ID = "ftbteams";
	public static final String MOD_NAME = "FTB Teams";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
	private static final List<String> teamArgumentSuggestions = new ArrayList<>();

	public FTBTeams()
	{
		FTBTeamsAPI.INSTANCE = new FTBTeamsAPIImpl();
		LifecycleEvent.SERVER_BEFORE_START.register(this::serverAboutToStart);
		CommandRegistrationEvent.EVENT.register(this::registerCommands);
		LifecycleEvent.SERVER_STOPPED.register(this::serverStopped);
		LifecycleEvent.SERVER_WORLD_SAVE.register(this::worldSaved);
		TeamConfigEvent.EVENT.register(this::teamConfig);
		PlayerEvent.PLAYER_JOIN.register(this::playerLoggedIn);
	}

	public void setup()
	{
		ArgumentTypes.register("ftbteams_team", TeamArgumentImpl.class, new EmptyArgumentSerializer<>(() -> new TeamArgumentImpl(() -> teamArgumentSuggestions)));
		ArgumentTypes.register("ftbteams_team_property", TeamPropertyArgument.class, new EmptyArgumentSerializer<>(TeamPropertyArgument::new));
	}

	private void serverAboutToStart(MinecraftServer server)
	{
		TeamManagerImpl.instance = new TeamManagerImpl(server);
		TeamManagerImpl.instance.load();
	}

	private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection selection)
	{
		new FTBTeamsCommands().register(dispatcher);
	}

	private void serverStopped(MinecraftServer server)
	{
		TeamManagerImpl.instance = null;
	}

	private void worldSaved(ServerLevel level)
	{
		if (TeamManagerImpl.instance != null)
		{
			TeamManagerImpl.instance.saveAll();
		}
	}

	private void teamConfig(TeamConfigEvent event)
	{
		event.add(TeamImpl.DISPLAY_NAME);
		event.add(TeamImpl.DESCRIPTION);
		event.add(TeamImpl.COLOR);
		event.add(TeamImpl.FREE_TO_JOIN);
	}

	private void playerLoggedIn(ServerPlayer player)
	{
		TeamManagerImpl manager = TeamManagerImpl.instance;

		if (manager != null && player instanceof ServerPlayer)
		{
			GameProfile profile = ProfileUtils.normalize(new GameProfile(player.getUUID(), player.getGameProfile().getName()));

			if (profile != ProfileUtils.NO_PROFILE && !manager.getKnownPlayers().contains(profile))
			{
				manager.getKnownPlayers().add(profile);

				try
				{
					manager.createPlayerTeam(player, "");
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}

				manager.save();
			}
		}
	}
}