package dev.ftb.mods.ftbteams.api.property;

import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Represents a keyed property value, including both the property instance, and a mutable value of that property.
 *
 * @param <T> the value type
 */
public final class TeamPropertyValue<T> {
	private final TeamProperty<T> property;
	private T value;

	public TeamPropertyValue(TeamProperty<T> property, T value) {
		this.property = property;
		this.value = value;
	}

	public TeamPropertyValue(TeamProperty<T> property) {
		this(property, property.getDefaultValue());
	}

	public static <X> TeamPropertyValue<X> fromNetwork(TeamProperty<X> property, RegistryFriendlyByteBuf buf) {
		return new TeamPropertyValue<>(property, property.readValue(buf));
	}

	public static <X> TeamPropertyValue<X> fromNBT(TeamProperty<X> property, Tag tag) {
		return new TeamPropertyValue<>(property, property.fromNBT(tag).orElse(property.getDefaultValue()));
	}

	public static <X> TeamPropertyValue<X> createDefaultValue(TeamProperty<X> property) {
		return new TeamPropertyValue<>(property, property.getDefaultValue());
	}

	public TeamProperty<T> getProperty() {
		return property;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public TeamPropertyValue<T> copy() {
		return new TeamPropertyValue<>(property, value);
	}

	@Override
	public String toString() {
		return property.id + ":" + value;
	}
}
