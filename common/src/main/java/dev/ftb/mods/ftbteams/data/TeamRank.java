package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public enum TeamRank implements StringRepresentable {
	ENEMY("enemy", -100),
	NONE("none", 0),
	ALLY("ally", 50, Icons.FRIENDS),
	INVITED("invited", 75),
	MEMBER("member", 100, Icons.ACCEPT_GRAY),
	OFFICER("officer", 500, Icons.SHIELD),
	OWNER("owner", 1000, Icons.DIAMOND),
	;

	public static final NameMap<TeamRank> NAME_MAP = NameMap.of(NONE, values()).create();

	private final String name;
	private final int power;
	private final Icon icon;

	TeamRank(String name, int power, Icon icon) {
		this.name = name;
		this.power = power;
		this.icon = icon;
	}

	TeamRank(String name, int power) {
		this(name, power, null);
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public int getPower() {
		return power;
	}

	public boolean is(TeamRank rank) {
		if (rank.power > 0) {
			return power >= rank.power;
		} else if (rank.power < 0) {
			return power <= rank.power;
		}

		return true;
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

	public boolean isInvited() {
		return is(INVITED);
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

	public Optional<Icon> getIcon() {
		return Optional.ofNullable(icon);
	}

	public Component getDisplayName() {
		return Component.translatable("ftbteams.ranks." + name);
	}
}
