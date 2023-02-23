package dev.ftb.mods.ftbteams.property;

import java.util.function.Consumer;

public final class TeamPropertyValue<T> {
	final TeamProperty<T> key;
	T value;
	Consumer<T> consumer;

	public TeamPropertyValue(TeamProperty<T> k, T v) {
		key = k;
		value = v;
		consumer = val -> value = val;
	}

	public TeamPropertyValue(TeamProperty<T> k) {
		this(k, k.defaultValue);
	}

	public TeamPropertyValue<T> copy() {
		return new TeamPropertyValue<>(key, value);
	}

	@Override
	public String toString() {
		return key.id + ":" + value;
	}
}
