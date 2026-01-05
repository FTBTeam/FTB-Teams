package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.function.Supplier;

public class IntProperty extends TeamProperty<Integer> {
	public final int minValue;
	public final int maxValue;

	public IntProperty(Identifier id, Supplier<Integer> def, int min, int max) {
		super(id, def);
		minValue = min;
		maxValue = max;
	}

	public IntProperty(Identifier id, Supplier<Integer> def) {
		this(id, def, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public IntProperty(Identifier id, int def, int min, int max) {
		this(id, () -> def, min, max);
	}

	public IntProperty(Identifier id, int def) {
		this(id, () -> def);
	}

	static IntProperty fromNetwork(Identifier id, FriendlyByteBuf buf) {
		return new IntProperty(id, buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
	}

	@Override
	public TeamPropertyType<Integer> getType() {
		return TeamPropertyType.INT;
	}

	@Override
	public Optional<Integer> fromString(String string) {
		try {
			int num = Integer.parseInt(string);
			return Optional.of(Mth.clamp(num, minValue, maxValue));
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	@Override
	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(getDefaultValue());
		buf.writeVarInt(minValue);
		buf.writeVarInt(maxValue);
	}

	@Override
	public void writeValue(RegistryFriendlyByteBuf buf, Integer value) {
		buf.writeInt(value);
	}

	@Override
	public Integer readValue(RegistryFriendlyByteBuf buf) {
		return buf.readInt();
	}

	@Override
	public ConfigValue<?> config(ConfigGroup config, TeamPropertyValue<Integer> value) {
		return config.addInt(id.getPath(), value.getValue(), value::setValue, getDefaultValue(), minValue, maxValue);
	}

	@Override
	public Tag toNBT(Integer value) {
		return IntTag.valueOf(value);
	}

	@Override
	public Optional<Integer> fromNBT(Tag tag) {
		if (tag instanceof NumericTag) {
			return Optional.of(Mth.clamp(tag.asInt().orElse(minValue), minValue, maxValue));
		}

		return Optional.empty();
	}
}
