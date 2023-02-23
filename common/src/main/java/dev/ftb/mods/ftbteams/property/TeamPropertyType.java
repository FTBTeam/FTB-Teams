package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.data.PrivacyMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamPropertyType<T> {
	private static final Map<String, TeamPropertyType<?>> MAP = new HashMap<>();

	public static final TeamPropertyType<Boolean> BOOLEAN = TeamPropertyType.register("boolean", BooleanProperty::new);
	public static final TeamPropertyType<String> STRING = TeamPropertyType.register("string", StringProperty::new);
	public static final TeamPropertyType<List<String>> STRING_LIST = TeamPropertyType.register("string_list", StringListProperty::new);
	public static final TeamPropertyType<Integer> INT = TeamPropertyType.register("int", IntProperty::new);
	public static final TeamPropertyType<Double> DOUBLE = TeamPropertyType.register("double", DoubleProperty::new);
	public static final TeamPropertyType<Color4I> COLOR = TeamPropertyType.register("color", ColorProperty::new);
	public static final TeamPropertyType<String> ENUM = TeamPropertyType.register("enum", EnumProperty::new);
	public static final TeamPropertyType<PrivacyMode> PRIVACY_MODE = TeamPropertyType.register("privacy_mode", PrivacyProperty::new);

	private final String id;
	private final FromNet<T> deserializer;

	private TeamPropertyType(String id, FromNet<T> deserializer) {
		this.id = id;
		this.deserializer = deserializer;
	}

	public static TeamProperty<?> read(FriendlyByteBuf buf) {
		return MAP.get(buf.readUtf(Short.MAX_VALUE)).deserializer.apply(buf.readResourceLocation(), buf);
	}

	public static void write(FriendlyByteBuf buf, TeamProperty<?> p) {
		buf.writeUtf(p.getType().id, Short.MAX_VALUE);
		buf.writeResourceLocation(p.id);
		p.write(buf);
	}

	private static <Y> TeamPropertyType<Y> register(String id, FromNet<Y> p) {
		TeamPropertyType<Y> t = new TeamPropertyType<>(id, p);
		MAP.put(id, t);
		return t;
	}

	public interface FromNet<Y> {
		TeamProperty<Y> apply(ResourceLocation id, FriendlyByteBuf buf);
	}
}
