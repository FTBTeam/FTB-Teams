package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.function.Supplier;

public class BooleanProperty extends TeamProperty<Boolean> {
	private static final Optional<Boolean> TRUE = Optional.of(Boolean.TRUE);
	private static final Optional<Boolean> FALSE = Optional.of(Boolean.FALSE);

	public BooleanProperty(Identifier id, Supplier<Boolean> def) {
		super(id, def);
	}

	public BooleanProperty(Identifier id, Boolean def) {
		this(id, () -> def);
	}

	static BooleanProperty fromNetwork(Identifier id, FriendlyByteBuf buf) {
		return new BooleanProperty(id, buf.readBoolean());
	}

	@Override
	public TeamPropertyType<Boolean> getType() {
		return TeamPropertyType.BOOLEAN;
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

	@Override
	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(getDefaultValue());
	}

	@Override
	public ConfigValue<?> config(ConfigGroup config, TeamPropertyValue<Boolean> value) {
		return config.addBool(id.getPath(), value.getValue(), value::setValue, getDefaultValue());
	}

	@Override
	public Tag toNBT(Boolean value) {
		return ByteTag.valueOf(value);
	}

	@Override
	public Optional<Boolean> fromNBT(Tag tag) {
        if (tag instanceof NumericTag) {
            if (tag.asByte().orElse((byte) 0) == 1) {
                return TRUE;
            }
        }

        return FALSE;
    }

	@Override
	public void writeValue(RegistryFriendlyByteBuf buf, Boolean value) {
		buf.writeBoolean(value);
	}

	@Override
	public Boolean readValue(RegistryFriendlyByteBuf buf) {
		return buf.readBoolean();
	}
}
