package com.feed_the_beast.mods.ftbteams.api.property;

import com.feed_the_beast.mods.ftbteams.api.TeamProperty;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

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
		if (string.length() == 6 || string.length() == 7 && string.startsWith("#"))
		{
			try
			{
				return Optional.of(Integer.valueOf(string.charAt(0) == '#' ? string.substring(1) : string, 16));
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
		return String.format("#%06x", value);
	}
}