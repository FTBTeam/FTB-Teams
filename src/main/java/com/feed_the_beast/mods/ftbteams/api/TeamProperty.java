package com.feed_the_beast.mods.ftbteams.api;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/**
 * @author LatvianModder
 */
public abstract class TeamProperty<T>
{
	public final ResourceLocation id;
	public final T defaultValue;
	private Predicate<Team> validator;

	public TeamProperty(ResourceLocation _id, T def)
	{
		id = _id;
		defaultValue = def;
		validator = team -> true;
	}

	public abstract Optional<T> fromString(String string);

	public String toString(T value)
	{
		return value.toString();
	}

	public final int hashCode()
	{
		return id.hashCode();
	}

	public final String toString()
	{
		return id.toString();
	}

	public final boolean equals(Object o)
	{
		if (o == this)
		{
			return true;
		}
		else if (o instanceof TeamProperty)
		{
			return id.equals(((TeamProperty) o).id);
		}

		return false;
	}

	public <P> P setValidator(Predicate<Team> predicate)
	{
		validator = predicate;
		return (P) this;
	}

	public boolean isValidFor(Team team)
	{
		return validator.test(team);
	}
}