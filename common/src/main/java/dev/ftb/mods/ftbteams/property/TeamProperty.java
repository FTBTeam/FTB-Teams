package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public abstract class TeamProperty<T> {
	protected final ResourceLocation id;
	protected final T defaultValue;

	public TeamProperty(ResourceLocation _id, T def) {
		id = _id;
		defaultValue = def;
	}

	public ResourceLocation getId() {
		return id;
	}

	public String getTranslationKey(String prefix) {
		return prefix + "." + id.getNamespace() + "." + id.getPath();
	}

	public abstract TeamPropertyType<T> getType();

	public abstract Optional<T> fromString(String string);

	public abstract void write(FriendlyByteBuf buf);

	public String toString(T value) {
		return value.toString();
	}

	public final int hashCode() {
		return id.hashCode();
	}

	public final String toString() {
		return id.toString();
	}

	public final boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (o instanceof TeamProperty) {
			return id.equals(((TeamProperty<?>) o).id);
		}

		return false;
	}

	public void writeValue(FriendlyByteBuf buf, T value) {
		buf.writeUtf(toString(value), Short.MAX_VALUE);
	}

	public T readValue(FriendlyByteBuf buf) {
		return fromString(buf.readUtf(Short.MAX_VALUE)).orElse(defaultValue);
	}

	public void config(ConfigGroup config, TeamPropertyValue<T> value) {
	}

	public Tag toNBT(T value) {
		return StringTag.valueOf(toString(value));
	}

	public Optional<T> fromNBT(Tag tag) {
		return fromString(tag.getAsString());
	}
}