package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftbteams.data.TeamProperty;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class StringProperty extends TeamProperty<String> {
	private final Pattern pattern;

	public StringProperty(ResourceLocation id, String def, @Nullable Pattern p) {
		super(id, def);
		pattern = p;
	}

	public StringProperty(ResourceLocation id, String def) {
		this(id, def, null);
	}

	@Override
	public Optional<String> fromString(String string) {
		if (pattern == null || pattern.matcher(string).matches()) {
			return Optional.of(string);
		}

		return Optional.empty();
	}
}