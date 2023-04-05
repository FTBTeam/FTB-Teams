package dev.ftb.mods.ftbteams.fabric;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyArgument;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.resources.ResourceLocation;

public class FTBTeamsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		new FTBTeams().setup();

		ArgumentTypeRegistry.registerArgumentType(new ResourceLocation(FTBTeamsAPI.MOD_ID, "team"), TeamArgument.class, new TeamArgument.Info());
		ArgumentTypeRegistry.registerArgumentType(new ResourceLocation(FTBTeamsAPI.MOD_ID, "team_property"), TeamPropertyArgument.class, new TeamPropertyArgument.Info());
	}
}
