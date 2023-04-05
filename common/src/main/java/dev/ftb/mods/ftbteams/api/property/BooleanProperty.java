package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
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

	public BooleanProperty(ResourceLocation id, FriendlyByteBuf buf) {
		super(id, buf.readBoolean());
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
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(defaultValue);
	}

	@Override
	public void config(ConfigGroup config, TeamPropertyValue<Boolean> value) {
		config.addBool(id.getPath(), value.value, value.consumer, defaultValue);
	}

	@Override
	public Tag toNBT(Boolean value) {
		return ByteTag.valueOf(value);
	}

	@Override
	public Optional<Boolean> fromNBT(Tag tag) {
		if (tag instanceof NumericTag) {
			if (((NumericTag) tag).getAsByte() == 1) {
				return TRUE;
			}

			return FALSE;
		}

		return Optional.empty();
	}
}