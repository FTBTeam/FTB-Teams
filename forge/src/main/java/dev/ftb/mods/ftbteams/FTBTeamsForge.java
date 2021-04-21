package dev.ftb.mods.ftbteams;

import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod(FTBTeams.MOD_ID)
public class FTBTeamsForge {
	public FTBTeamsForge() {
		EventBuses.registerModEventBus(FTBTeams.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		dev.ftb.mods.ftbteams.FTBTeams teams = new FTBTeams();
		FMLJavaModLoadingContext.get().getModEventBus().<FMLCommonSetupEvent>addListener(event -> teams.setup());
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	}
}
