package dev.ftb.mods.ftbteams;

import net.fabricmc.api.ModInitializer;

public class FTBTeamsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		new FTBTeams().setup();
	}
}
