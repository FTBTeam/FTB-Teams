package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftbguilibrary.config.ConfigGroup;
import dev.ftb.mods.ftbteams.data.PrivacyMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class PrivacyProperty extends TeamProperty<PrivacyMode> {
	public PrivacyProperty(ResourceLocation id, PrivacyMode def) {
		super(id, def);
	}

	public PrivacyProperty(ResourceLocation id, FriendlyByteBuf buf) {
		super(id, PrivacyMode.VALUES[buf.readVarInt()]);
	}

	@Override
	public TeamPropertyType<PrivacyMode> getType() {
		return TeamPropertyType.PRIVACY_MODE;
	}

	@Override
	public Optional<PrivacyMode> fromString(String string) {
		return Optional.ofNullable(PrivacyMode.NAME_MAP.getNullable(string));
	}

	@Override
	public String toString(PrivacyMode value) {
		return value.getSerializedName();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(defaultValue.ordinal());
	}

	@Override
	public void config(ConfigGroup config, TeamPropertyValue<PrivacyMode> value) {
		config.addEnum(id.getNamespace() + "." + id.getPath(), value.value, value.consumer, PrivacyMode.NAME_MAP);
	}
}