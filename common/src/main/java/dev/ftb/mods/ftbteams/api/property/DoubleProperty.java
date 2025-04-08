package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.function.Supplier;

public class DoubleProperty extends TeamProperty<Double> {
	public final double minValue;
	public final double maxValue;

	public DoubleProperty(ResourceLocation id, Supplier<Double> def, double min, double max) {
		super(id, def);
		minValue = min;
		maxValue = max;
	}

	public DoubleProperty(ResourceLocation id, double def, double min, double max) {
		this(id, () -> def, min, max);
	}

	public DoubleProperty(ResourceLocation id, Supplier<Double> def) {
		this(id, def, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	static DoubleProperty fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
		return new DoubleProperty(id, buf.readDouble(), buf.readDouble(), buf.readDouble());
	}

	@Override
	public TeamPropertyType<Double> getType() {
		return TeamPropertyType.DOUBLE;
	}

	@Override
	public Optional<Double> fromString(String string) {
		try {
			double num = Double.parseDouble(string);
			return Optional.of(Mth.clamp(num, minValue, maxValue));
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	@Override
	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeDouble(getDefaultValue());
		buf.writeDouble(minValue);
		buf.writeDouble(maxValue);
	}

	@Override
	public ConfigValue<?> config(ConfigGroup config, TeamPropertyValue<Double> value) {
		return config.addDouble(id.getPath(), value.getValue(), value::setValue, getDefaultValue(), minValue, maxValue);
	}

	@Override
	public Tag toNBT(Double value) {
		return DoubleTag.valueOf(value);
	}

	@Override
	public Optional<Double> fromNBT(Tag tag) {
		if (tag instanceof NumericTag) {
			return Optional.of(Mth.clamp(tag.asDouble().orElse(minValue), minValue, maxValue));
		}

	@Override
	public Double readValue(RegistryFriendlyByteBuf buf) {
		return buf.readDouble();
	}

	@Override
	public void writeValue(RegistryFriendlyByteBuf buf, Double value) {
		buf.writeDouble(value);
	}
}
