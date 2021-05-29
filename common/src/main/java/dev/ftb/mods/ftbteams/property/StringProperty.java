package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

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

	public StringProperty(ResourceLocation id, FriendlyByteBuf buf) {
		super(id, buf.readUtf(Short.MAX_VALUE));
		int f = buf.readVarInt();
		String s = buf.readUtf(Short.MAX_VALUE);
		pattern = s.isEmpty() ? null : Pattern.compile(s, f);
	}

	@Override
	public TeamPropertyType<String> getType() {
		return TeamPropertyType.STRING;
	}

	@Override
	public Optional<String> fromString(String string) {
		if (pattern == null || pattern.matcher(string).matches()) {
			return Optional.of(string);
		}

		return Optional.empty();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(defaultValue, Short.MAX_VALUE);
		buf.writeVarInt(pattern == null ? 0 : pattern.flags());
		buf.writeUtf(pattern == null ? "" : pattern.pattern(), Short.MAX_VALUE);
	}

	@Override
	public void config(ConfigGroup config, TeamPropertyValue<String> value) {
		config.addString(id.getNamespace() + "." + id.getPath(), value.value, value.consumer, defaultValue, pattern);
	}
}