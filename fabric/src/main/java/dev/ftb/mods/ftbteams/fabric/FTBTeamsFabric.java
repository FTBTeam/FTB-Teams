package dev.ftb.mods.ftbteams.fabric;

import dev.ftb.mods.ftbteams.FTBTeams;
import net.fabricmc.api.ModInitializer;

public class FTBTeamsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		new FTBTeams().setup();
	}
}
