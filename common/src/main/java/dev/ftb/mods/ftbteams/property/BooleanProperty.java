package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftbteams.data.TeamProperty;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class BooleanProperty extends TeamProperty<Boolean> {
	private static final Optional<Boolean> TRUE = Optional.of(Boolean.TRUE);
	private static final Optional<Boolean> FALSE = Optional.of(Boolean.FALSE);

	public BooleanProperty(ResourceLocation id, Boolean def) {
		super(id, def);
	}

	@Override
	public Optional<Boolean> fromString(String string) {
		if (string.equals("true")) {
			return TRUE;
		} else if (string.equals("false")) {
			return FALSE;
		}

		return Optional.empty();
	}
}