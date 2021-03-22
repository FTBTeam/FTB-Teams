package com.feed_the_beast.mods.ftbteams.data;

public class ServerTeam extends Team {
	public ServerTeam(TeamManager m) {
		super(m);
	}

	@Override
	public TeamType getType() {
		return TeamType.SERVER;
	}
}
