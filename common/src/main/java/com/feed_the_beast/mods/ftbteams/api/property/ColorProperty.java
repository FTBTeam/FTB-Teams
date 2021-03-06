package com.feed_the_beast.mods.ftbteams.api.property;

import com.feed_the_beast.mods.ftbteams.api.TeamProperty;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class ColorProperty extends TeamProperty<Integer>
{
	public ColorProperty(ResourceLocation id, int def)
	{
		super(id, def);
	}

	@Override
	public Optional<Integer> fromString(String string)
	{
		if (string.length() >= 7 && string.startsWith("#"))
		{
			try
			{
				long col = Long.decode(string) & 0xFFFFFFL;
				return Optional.of((int) col);
			}
			catch (Exception ex)
			{
			}
		}

		return Optional.empty();
	}

	@Override
	public String toString(Integer value)
	{
		return String.format("#%06X", value & 0xFFFFFF);
	}
}