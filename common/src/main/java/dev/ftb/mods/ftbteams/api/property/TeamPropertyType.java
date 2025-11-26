package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the unique type for a {@link TeamProperty}. This is distinct from the property <em>value</em> type, and is
 * used for network encoding/decoding.
 *
 * @param <T>
 */
public class TeamPropertyType<T> {
	private static final Map<ResourceLocation, TeamPropertyType<?>> MAP = new ConcurrentHashMap<>();

	// builtin types
	public static final TeamPropertyType<Boolean> BOOLEAN = register("boolean", BooleanProperty::fromNetwork);
	public static final TeamPropertyType<String> STRING = register("string", StringProperty::fromNetwork);
	public static final TeamPropertyType<List<String>> STRING_LIST = register("string_list", StringListProperty::fromNetwork);
	public static final TeamPropertyType<Integer> INT = register("int", IntProperty::fromNetwork);
	public static final TeamPropertyType<Double> DOUBLE = register("double", DoubleProperty::fromNetwork);
	public static final TeamPropertyType<Color4I> COLOR = register("color", ColorProperty::fromNetwork);
	public static final TeamPropertyType<String> ENUM = register("enum", EnumProperty::fromNetwork);
	public static final TeamPropertyType<PrivacyMode> PRIVACY_MODE = register("privacy_mode", PrivacyProperty::fromNetwork);
	public static final TeamPropertyType<BigInteger> BIG_INTEGER = register("big_integer", BigIntegerProperty::fromNetwork);

	private final ResourceLocation id;
	private final FromNet<T> deserializer;

	private TeamPropertyType(ResourceLocation id, FromNet<T> deserializer) {
		this.id = id;
		this.deserializer = deserializer;
	}

	public static TeamProperty<?> read(RegistryFriendlyByteBuf buf) {
		ResourceLocation typeId = buf.readResourceLocation();
		ResourceLocation propId = buf.readResourceLocation();
		boolean playerEditable = buf.readBoolean();
		TeamProperty<?> prop = MAP.get(typeId).deserializer.apply(propId, buf);
		return playerEditable ? prop : prop.notPlayerEditable();
	}

	public static void write(RegistryFriendlyByteBuf buf, TeamProperty<?> prop) {
		buf.writeResourceLocation(prop.getType().id);
		buf.writeResourceLocation(prop.id);
		buf.writeBoolean(prop.isPlayerEditable());
		prop.write(buf);
	}

	private static <Y> TeamPropertyType<Y> register(String id, FromNet<Y> deserializer) {
		return register(FTBTeamsAPI.rl(id), deserializer);
	}

	/**
	 * Register a new type. This is safe to do via a static initializer.
	 *
	 * @param id the type ID
	 * @param deserializer the property deserializer, which must be able to read a property from the network
	 * @return the type that has just been registered
	 */
	public static <Y> TeamPropertyType<Y> register(ResourceLocation id, FromNet<Y> deserializer) {
		TeamPropertyType<Y> t = new TeamPropertyType<>(id, deserializer);
		MAP.put(id, t);
		return t;
	}

	public interface FromNet<Y> {
		TeamProperty<Y> apply(ResourceLocation id, RegistryFriendlyByteBuf buf);
	}
}
