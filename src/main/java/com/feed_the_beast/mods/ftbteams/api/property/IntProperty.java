package com.feed_the_beast.mods.ftbteams.api.property;

import com.feed_the_beast.mods.ftbteams.api.TeamProperty;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class IntProperty extends TeamProperty<Integer>
{
	public final int minValue;
	public final int maxValue;

	public IntProperty(ResourceLocation id, int def, int min, int max)
	{
		super(id, def);
		minValue = min;
		maxValue = max;
	}

	public IntProperty(ResourceLocation id, int def)
	{
		this(id, def, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public Optional<Integer> fromString(String string)
	{
		try
		{
			int num = Integer.parseInt(string);
			return Optional.of(MathHelper.clamp(num, minValue, maxValue));
		}
		catch (Exception ex)
		{
			return Optional.empty();
		}
	}
}