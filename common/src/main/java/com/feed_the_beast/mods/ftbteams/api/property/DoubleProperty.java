package com.feed_the_beast.mods.ftbteams.api.property;

import com.feed_the_beast.mods.ftbteams.api.TeamProperty;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * @author LatvianModder
 */
public class DoubleProperty extends TeamProperty<Double>
{
	public final double minValue;
	public final double maxValue;

	public DoubleProperty(ResourceLocation id, double def, double min, double max)
	{
		super(id, def);
		minValue = min;
		maxValue = max;
	}

	public DoubleProperty(ResourceLocation id, double def)
	{
		this(id, def, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	@Override
	public Optional<Double> fromString(String string)
	{
		try
		{
			double num = Double.parseDouble(string);
			return Optional.of(Mth.clamp(num, minValue, maxValue));
		}
		catch (Exception ex)
		{
			return Optional.empty();
		}
	}
}