package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableConfigValue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.function.Supplier;

// TODO this should be moved to FTB Chunks
public class PrivacyProperty extends TeamProperty<PrivacyMode> {
	public PrivacyProperty(Identifier id, Supplier<PrivacyMode> def) {
		super(id, def);
	}

	public PrivacyProperty(Identifier id, PrivacyMode def) {
		this(id, () -> def);
	}

	static PrivacyProperty fromNetwork(Identifier id, FriendlyByteBuf buf) {
		return new PrivacyProperty(id, buf.readEnum(PrivacyMode.class));
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
	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeEnum(getDefaultValue());
	}

	@Override
	public EditableConfigValue<?> config(EditableConfigGroup config, TeamPropertyValue<PrivacyMode> value) {
		return config.addEnum(id.getPath(), value.getValue(), value::setValue, PrivacyMode.NAME_MAP);
	}
}
