package dev.ftb.mods.ftbteams.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyArgument;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static net.minecraftforge.fml.IExtensionPoint.DisplayTest;

@Mod(FTBTeamsAPI.MOD_ID)
public class FTBTeamsForge {
	private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, FTBTeamsAPI.MOD_ID);
	private static final RegistryObject<TeamArgument.Info> TEAM_ARGUMENT = COMMAND_ARGUMENT_TYPES.register("team", () -> ArgumentTypeInfos.registerByClass(TeamArgument.class, new TeamArgument.Info()));
	private static final RegistryObject<TeamPropertyArgument.Info> TEAM_PROPERTY_ARGUMENT = COMMAND_ARGUMENT_TYPES.register("team_property", () -> ArgumentTypeInfos.registerByClass(TeamPropertyArgument.class, new TeamPropertyArgument.Info()));

	public FTBTeamsForge() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		EventBuses.registerModEventBus(FTBTeamsAPI.MOD_ID, modEventBus);
		COMMAND_ARGUMENT_TYPES.register(modEventBus);

		new FTBTeams();
		
//		FMLJavaModLoadingContext.get().getModEventBus().<FMLCommonSetupEvent>addListener(event -> teams.setup());
		ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	}
}
