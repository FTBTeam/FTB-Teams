package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * @author LatvianModder
 */
public class EnumProperty extends TeamProperty<String> {
	private final List<String> values;
	private final Map<String, Component> names;

	public EnumProperty(ResourceLocation id, String def, List<String> v, Map<String, Component> n) {
		super(id, def);
		values = v;
		names = n;
	}

	public <T> EnumProperty(ResourceLocation id, NameMap<T> nameMap) {
		super(id, nameMap.getName(nameMap.defaultValue));
		values = nameMap.keys;
		names = new HashMap<>();

		for (T val : nameMap) {
			names.put(nameMap.getName(val), nameMap.getDisplayName(val));
		}
	}

	public EnumProperty(ResourceLocation id, FriendlyByteBuf buf) {
		super(id, buf.readUtf(Short.MAX_VALUE));
		int sv = buf.readVarInt();
		values = new ArrayList<>(sv);

		for (int i = 0; i < sv; i++) {
			values.add(buf.readUtf(Short.MAX_VALUE));
		}

		int sn = buf.readVarInt();
		names = new HashMap<>(sn);

		for (int i = 0; i < sn; i++) {
			names.put(buf.readUtf(Short.MAX_VALUE), buf.readComponent());
		}
	}

	@Override
	public TeamPropertyType<String> getType() {
		return TeamPropertyType.ENUM;
	}

	@Override
	public Optional<String> fromString(String string) {
		return Optional.of(string);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(defaultValue, Short.MAX_VALUE);
		buf.writeVarInt(values.size());

		for (String s : values) {
			buf.writeUtf(s, Short.MAX_VALUE);
		}

		buf.writeVarInt(names.size());

		for (Map.Entry<String, Component> s : names.entrySet()) {
			buf.writeUtf(s.getKey(), Short.MAX_VALUE);
			buf.writeComponent(s.getValue());
		}
	}

	@Override
	public void config(ConfigGroup config, TeamPropertyValue<String> value) {
		config.addEnum(id.getPath(), value.value, value.consumer, NameMap.of(defaultValue, values).name(s -> names.getOrDefault(s, Component.literal(s))).create());
	}

	@Override
	public Tag toNBT(String value) {
		return StringTag.valueOf(value);
	}

	@Override
	public Optional<String> fromNBT(Tag tag) {
		if (tag instanceof StringTag) {
			return Optional.of(tag.getAsString());
		}

		return Optional.empty();
	}
}
