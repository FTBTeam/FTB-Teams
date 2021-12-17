package dev.ftb.mods.ftbteams.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.ftb.mods.ftbteams.FTBTeams;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;

import static net.minecraftforge.fml.IExtensionPoint.DisplayTest;

@Mod(FTBTeams.MOD_ID)
public class FTBTeamsForge {
	public FTBTeamsForge() {
		EventBuses.registerModEventBus(FTBTeams.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		FTBTeams teams = new FTBTeams();
		FMLJavaModLoadingContext.get().getModEventBus().<FMLCommonSetupEvent>addListener(event -> teams.setup());
		ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	}
}
