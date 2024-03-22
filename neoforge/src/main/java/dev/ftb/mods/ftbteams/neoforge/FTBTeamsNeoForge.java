package dev.ftb.mods.ftbteams.neoforge;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.IExtensionPoint.DisplayTest;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

@Mod(FTBTeamsAPI.MOD_ID)
public class FTBTeamsNeoForge {
	public FTBTeamsNeoForge(IEventBus modEventBus) {
		ArgumentTypes.COMMAND_ARGUMENT_TYPES.register(modEventBus);

		new FTBTeams();

		ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new DisplayTest(() -> DisplayTest.IGNORESERVERONLY, (a, b) -> true));
	}
}
