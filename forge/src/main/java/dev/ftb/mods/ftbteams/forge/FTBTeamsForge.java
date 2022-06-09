package dev.ftb.mods.ftbteams.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import dev.ftb.mods.ftbteams.property.TeamPropertyArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static net.minecraftforge.fml.IExtensionPoint.DisplayTest;

@Mod(FTBTeams.MOD_ID)
public class FTBTeamsForge {
	private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY, FTBTeams.MOD_ID);
	private static final RegistryObject<TeamArgument.Info> TEAM_ARGUMENT = COMMAND_ARGUMENT_TYPES.register("team", () -> ArgumentTypeInfos.registerByClass(TeamArgument.class, new TeamArgument.Info()));
	private static final RegistryObject<TeamPropertyArgument.Info> TEAM_PROPERTY_ARGUMENT = COMMAND_ARGUMENT_TYPES.register("team_property", () -> ArgumentTypeInfos.registerByClass(TeamPropertyArgument.class, new TeamPropertyArgument.Info()));

	public FTBTeamsForge() {
		EventBuses.registerModEventBus(FTBTeams.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		new FTBTeams();

//		FMLJavaModLoadingContext.get().getModEventBus().<FMLCommonSetupEvent>addListener(event -> teams.setup());
		ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	}
}
