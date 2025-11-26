package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents an individual property; the combination of a unique ID, and the default value for the property.
 * Properties should be declared statically by a mod (see {@link TeamProperties} as an example), and registered via
 * the {@link dev.ftb.mods.ftbteams.api.event.TeamCollectPropertiesEvent} event.
 *
 * @param <T> the type of value that the property holds
 */
public abstract class TeamProperty<T> {
	protected final ResourceLocation id;
	private final Supplier<T> defaultValue;
	private boolean playerEditable;
	private boolean shouldSyncToAll;

	protected TeamProperty(ResourceLocation id, Supplier<T> defaultValue) {
		this.id = id;
		this.defaultValue = defaultValue;

		playerEditable = true;
		shouldSyncToAll = false;
	}

	protected TeamProperty(ResourceLocation id, T defaultValue) {
		this(id, () -> defaultValue);
	}

	/**
	 * Get the unique type for this property, which must have already been registered
	 * via {@link TeamPropertyType#register(ResourceLocation, TeamPropertyType.FromNet)}.
	 *
	 * @return the property type
	 */
	public abstract TeamPropertyType<T> getType();

	/**
	 * {@return the unique ID for this instance, defined when the property was declared, and namespaced within the mod
	 * that registered it}
	 */
	public ResourceLocation getId() {
		return id;
	}

	/**
	 * {@return the default value for this property instance, defined when the property was declared}
	 */
	public T getDefaultValue() {
		return defaultValue.get();
	}

	/**
	 * {@return if the property may be edited by players of the team, either via clientside GUI, or the serverside
	 * "/ftbteams settings" command}
	 */
	public boolean isPlayerEditable() {
		return playerEditable;
	}

	/**
	 * Mark a property as not player-editable (see {@link #isPlayerEditable()}). Should be called during property
	 * declaration.
	 *
	 * @return the same property
	 */
	public TeamProperty<T> notPlayerEditable() {
		playerEditable = false;
		return this;
	}

	/**
	 * {@return if the property should be sync'd to all players when changed; if false, then the property is sync'd
	 * only to members of the team to which the property belongs}
	 */
	public boolean shouldSyncToAll() {
		return shouldSyncToAll;
	}

	/**
	 * Declare a property as sync'd to all (see {@link #shouldSyncToAll()}). By default, properties are not sync'd to
	 * all players, only to members of the team owning the property. Use this if a team property needs to be visible
	 * to all clients (e.g. team color and title are both marked as sync-to-all).
	 *
	 * @return the same property
	 */
	public TeamProperty<T> syncToAll() {
		shouldSyncToAll = true;
		return this;
	}

	public String getTranslationKey(String prefix) {
		return prefix + "." + id.getNamespace() + "." + id.getPath();
	}

	/**
	 * Deserialize the property's value from a string. This should act as the exact opposite of {@link #toString(T)},
	 * so that {@code prop.fromString(prop.toString(val)).get()} returns {@code val}.
	 *
	 * @param string the string to parse
	 * @return an optional of the value, or {@code Optional.empty()} if the string could not be parsed
	 */
	public abstract Optional<T> fromString(String string);

	/**
	 * Write this property to the network. This writes the meta-information for the property itself, not any
	 * particular value of the property (see also {@link #writeValue(RegistryFriendlyByteBuf, T)}).
	 * <p>
	 * This method should <em>not</em> write the property ID ({@link #getId()}).
	 *
	 * @param buf the buffer
	 */
	public abstract void write(RegistryFriendlyByteBuf buf);

	/**
	 * Write the given value to a string, suitable for later parsing by {@link #fromString(String)}. This is
	 * <em>not</em> the same as {@link #toString()} !
	 *
	 * @param value the value to write
	 * @return a string result
	 */
	public String toString(T value) {
		return value.toString();
	}

	/**
	 * Write one value of this property to the network. The default implementation (stringify, and write the string)
	 * should work for all property types, but is typically not the most efficient way to do it.
	 *
	 * @param buf the network buffer
	 * @param value the value to write
 	 */
	public void writeValue(RegistryFriendlyByteBuf buf, T value) {
		buf.writeUtf(toString(value), Short.MAX_VALUE);
	}

	/**
	 * Read one value of this property from the network.
	 *
	 * @param buf the network buffer
	 * @return the value that has been read
	 */
	public T readValue(RegistryFriendlyByteBuf buf) {
		return fromString(buf.readUtf(Short.MAX_VALUE)).orElse(getDefaultValue());
	}

	/**
	 * Write one value of this property to the network. The default implementation (stringify, and write the string to
	 * a {@code StringTag}) should work for all property types, but is typically not the most efficient way to do it.
	 *
	 * @param value the value to write
	 * @return the tag
	 */
	public Tag toNBT(T value) {
		return StringTag.valueOf(toString(value));
	}

	/**
	 * Read one value of this property from a NBT tag.
	 *
	 * @param tag the tag to read from
	 * @return the value that has been read
	 */
	public Optional<T> fromNBT(Tag tag) {
		return fromString(tag.getAsString());
	}

	/**
	 * Add this property to a {@code ConfigGroup} object, to allow interactive configuration via GUI. Returning null
	 * here is valid, but properties of this type will not be GUI-editable.
	 *
	 * @param config the config group to add the property to
	 * @param value  the property value to be added
	 * @return the added config value, or null if no config value was added to the config group
	 */
	public @Nullable ConfigValue<?> config(ConfigGroup config, TeamPropertyValue<T> value) {
		return null;
	}

	@Override
	public final boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (o instanceof TeamProperty) {
			return id.equals(((TeamProperty<?>) o).id);
		}

		return false;
	}

	@Override
	public final int hashCode() {
		return id.hashCode();
	}

	@Override
	public final String toString() {
		return id.toString();
	}

	@Deprecated(forRemoval = true)
	public TeamPropertyValue<T> createDefaultValue() {
		return new TeamPropertyValue<>(this, getDefaultValue());
	}

	@Deprecated(forRemoval = true)
	public TeamPropertyValue<T> createValueFromNetwork(RegistryFriendlyByteBuf buf) {
		return new TeamPropertyValue<>(this, readValue(buf));
	}

	@Deprecated(forRemoval = true)
	public TeamPropertyValue<T> createValueFromNBT(Tag tag) {
		return new TeamPropertyValue<>(this, fromNBT(tag).orElse(getDefaultValue()));
	}
}