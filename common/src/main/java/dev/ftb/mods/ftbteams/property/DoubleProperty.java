package dev.ftb.mods.ftbteams.property;

import com.feed_the_beast.mods.ftbguilibrary.config.ConfigGroup;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class DoubleProperty extends TeamProperty<Double> {
	public final double minValue;
	public final double maxValue;

	public DoubleProperty(ResourceLocation id, double def, double min, double max) {
		super(id, def);
		minValue = min;
		maxValue = max;
	}

	public DoubleProperty(ResourceLocation id, double def) {
		this(id, def, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	public DoubleProperty(ResourceLocation id, FriendlyByteBuf buf) {
		super(id, buf.readDouble());
		minValue = buf.readDouble();
		maxValue = buf.readDouble();
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
	public void write(FriendlyByteBuf buf) {
		buf.writeDouble(defaultValue);
		buf.writeDouble(minValue);
		buf.writeDouble(maxValue);
	}

	@Override
	public void config(ConfigGroup config, TeamPropertyValue<Double> value) {
		config.addDouble(id.getNamespace() + "." + id.getPath(), value.value, value.consumer, defaultValue, minValue, maxValue);
	}

	@Override
	public Tag toNBT(Double value) {
		return DoubleTag.valueOf(value);
	}

	@Override
	public Optional<Double> fromNBT(Tag tag) {
		if (tag instanceof NumericTag) {
			return Optional.of(Mth.clamp(((NumericTag) tag).getAsDouble(), minValue, maxValue));
		}

		return Optional.empty();
	}
}