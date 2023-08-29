package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Supplier;

public class EnumProperty extends TeamProperty<String> {
	private final List<String> values;
	private final Map<String, Component> names;

	public EnumProperty(ResourceLocation id, Supplier<String> def, List<String> values, Map<String,Component> names) {
		super(id, def);
		this.values = values;
		this.names = names;
	}

	public <T> EnumProperty(ResourceLocation id, NameMap<T> nameMap) {
		this(id, () -> nameMap.getName(nameMap.defaultValue), nameMap.keys, buildMap(nameMap));
	}

	private static <T> Map<String,Component> buildMap(NameMap<T> nameMap) {
		Map<String,Component> res = new HashMap<>();
		nameMap.forEach(val -> res.put(nameMap.getName(val), nameMap.getDisplayName(val)));
		return res;
	}

	static EnumProperty fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
		String def = buf.readUtf(Short.MAX_VALUE);
		List<String> values = buf.readList(b -> b.readUtf(Short.MAX_VALUE));
		Map<String,Component> names = buf.readMap(b -> b.readUtf(Short.MAX_VALUE), FriendlyByteBuf::readComponent);
		return new EnumProperty(id, () -> def, values, names);
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
		buf.writeUtf(getDefaultValue(), Short.MAX_VALUE);
		buf.writeCollection(values, FriendlyByteBuf::writeUtf);
		buf.writeMap(names, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeComponent);
	}

	@Override
	public void config(ConfigGroup config, TeamPropertyValue<String> value) {
		config.addEnum(id.getPath(), value.value, value.consumer, NameMap.of(getDefaultValue(), values).name(s -> names.getOrDefault(s, Component.literal(s))).create());
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
