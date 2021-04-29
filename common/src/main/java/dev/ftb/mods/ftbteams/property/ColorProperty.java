package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class ColorProperty extends TeamProperty<Color4I> {
	public ColorProperty(ResourceLocation id, Color4I def) {
		super(id, def);
	}

	public ColorProperty(ResourceLocation id, FriendlyByteBuf def) {
		super(id, Color4I.rgb(def.readVarInt()));
	}

	@Override
	public TeamPropertyType<Color4I> getType() {
		return TeamPropertyType.COLOR;
	}

	@Override
	public Optional<Color4I> fromString(String string) {
		Color4I c = Color4I.fromString(string);
		return c.isEmpty() ? Optional.empty() : Optional.of(c);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(defaultValue.rgb());
	}

	@Override
	public String toString(Color4I value) {
		return value.toString();
	}

	@Override
	public void config(ConfigGroup config, TeamPropertyValue<Color4I> value) {
		config.addString(id.getNamespace() + "." + id.getPath(), value.value.toString(), s -> value.consumer.accept(Color4I.fromString(s)), defaultValue.toString());
	}
}