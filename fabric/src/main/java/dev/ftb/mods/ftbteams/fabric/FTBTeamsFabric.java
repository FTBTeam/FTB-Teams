package dev.ftb.mods.ftbteams.fabric;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyArgument;
import dev.ftb.mods.ftbteams.command.TeamArgument;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

public class FTBTeamsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		new FTBTeams();

		ArgumentTypeRegistry.registerArgumentType(FTBTeamsAPI.id("team"), TeamArgument.class, new TeamArgument.Info());
		ArgumentTypeRegistry.registerArgumentType(FTBTeamsAPI.id("team_property"), TeamPropertyArgument.class, new TeamPropertyArgument.Info());
	}
}
