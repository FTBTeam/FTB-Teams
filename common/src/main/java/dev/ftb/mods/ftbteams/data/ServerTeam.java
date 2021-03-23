package dev.ftb.mods.ftbteams.data;

public class ServerTeam extends Team {
	public ServerTeam(TeamManager m) {
		super(m);
	}

	@Override
	public TeamType getType() {
		return TeamType.SERVER;
	}
}
