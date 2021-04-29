package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftblibrary.config.NameMap;
import net.minecraft.util.StringRepresentable;

/**
 * @author LatvianModder
 */
public enum PrivacyMode implements StringRepresentable {
	ALLIES("allies"),
	PRIVATE("private"),
	PUBLIC("public");

	public static final PrivacyMode[] VALUES = values();
	public static final NameMap<PrivacyMode> NAME_MAP = NameMap.of(ALLIES, VALUES).baseNameKey("ftbteams.privacy_mode").create();

	public final String name;

	PrivacyMode(String n) {
		name = n;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}