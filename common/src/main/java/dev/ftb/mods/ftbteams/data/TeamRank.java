package dev.ftb.mods.ftbteams.data;

import com.feed_the_beast.mods.ftbguilibrary.config.NameMap;
import net.minecraft.util.StringRepresentable;

public enum TeamRank implements StringRepresentable {
	ENEMY("enemy", -100),
	NONE("none", 0),
	ALLY("ally", 50),
	INVITED("invited", 75),
	MEMBER("member", 100),
	OFFICER("officer", 500),
	OWNER("owner", 1000),

	;

	public static final NameMap<TeamRank> NAME_MAP = NameMap.of(NONE, values()).create();

	private final String name;
	private final int power;

	TeamRank(String n, int p) {
		name = n;
		power = p;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public int getPower() {
		return power;
	}

	public boolean is(TeamRank rank) {
		return power >= rank.power;
	}

	public boolean isEnemy() {
		return is(ENEMY);
	}

	public boolean isNone() {
		return is(NONE);
	}

	public boolean isAlly() {
		return is(ALLY);
	}

	public boolean isMember() {
		return is(MEMBER);
	}

	public boolean isOfficer() {
		return is(OFFICER);
	}

	public boolean isOwner() {
		return is(OWNER);
	}
}
