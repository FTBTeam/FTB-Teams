package com.feed_the_beast.mods.ftbteams.api.property;

import com.feed_the_beast.mods.ftbteams.api.TeamProperty;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * @author LatvianModder
 */
public class BooleanProperty extends TeamProperty<Boolean>
{
	private static final Optional<Boolean> TRUE = Optional.of(Boolean.TRUE);
	private static final Optional<Boolean> FALSE = Optional.of(Boolean.FALSE);

	public BooleanProperty(ResourceLocation id, Boolean def)
	{
		super(id, def);
	}

	@Override
	public Optional<Boolean> fromString(String string)
	{
		if (string.equals("true"))
		{
			return TRUE;
		}
		else if (string.equals("false"))
		{
			return FALSE;
		}

		return Optional.empty();
	}
}