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
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(FTBTeams.MOD_ID)
public class FTBTeams
{
	public static final String MOD_ID = "ftbteams";
	public static final String MOD_NAME = "FTB Teams";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
	private static final List<String> teamArgumentSuggestions = new ArrayList<>();

	public FTBTeams()
	{
		FTBTeamsAPI.INSTANCE = new FTBTeamsAPIImpl();
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		MinecraftForge.EVENT_BUS.addListener(this::serverAboutToStart);
		MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
		MinecraftForge.EVENT_BUS.addListener(this::serverStopped);
		MinecraftForge.EVENT_BUS.addListener(this::worldSaved);
		MinecraftForge.EVENT_BUS.addListener(this::teamConfig);
		MinecraftForge.EVENT_BUS.addListener(this::playerLoggedIn);
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	}

	private void setup(FMLCommonSetupEvent event)
	{
		ArgumentTypes.register("ftbteams_team", TeamArgumentImpl.class, new EmptyArgumentSerializer<>(() -> new TeamArgumentImpl(() -> teamArgumentSuggestions)));
		ArgumentTypes.register("ftbteams_team_property", TeamPropertyArgument.class, new EmptyArgumentSerializer<>(TeamPropertyArgument::new));
	}

	private void serverAboutToStart(FMLServerAboutToStartEvent event)
	{
		TeamManagerImpl.instance = new TeamManagerImpl(event.getServer());
		TeamManagerImpl.instance.load();
	}

	private void registerCommands(RegisterCommandsEvent event)
	{
		new FTBTeamsCommands().register(event.getDispatcher());
	}

	private void serverStopped(FMLServerStoppedEvent event)
	{
		TeamManagerImpl.instance = null;
	}

	private void worldSaved(WorldEvent.Save event)
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

	private void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		TeamManagerImpl manager = TeamManagerImpl.instance;

		if (manager != null && event.getPlayer() instanceof ServerPlayer)
		{
			GameProfile profile = ProfileUtils.normalize(new GameProfile(event.getPlayer().getUUID(), event.getPlayer().getGameProfile().getName()));

			if (profile != ProfileUtils.NO_PROFILE && !manager.getKnownPlayers().contains(profile))
			{
				manager.getKnownPlayers().add(profile);

				try
				{
					manager.createPlayerTeam((ServerPlayer) event.getPlayer(), "");
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