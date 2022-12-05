package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftbteams.event.TeamCollectPropertiesEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class TeamProperties {
	public final Map<TeamProperty, TeamPropertyValue> map = new LinkedHashMap<>();

	public TeamProperties collect() {
		map.clear();
		TeamEvent.COLLECT_PROPERTIES.invoker().accept(new TeamCollectPropertiesEvent(prop -> map.put(prop, new TeamPropertyValue(prop, prop.defaultValue))));
		return this;
	}

	public TeamProperties copy() {
		TeamProperties p = new TeamProperties();

		map.forEach((key, value) -> p.map.put(key, value.copy()));

		return p;
	}

	public TeamProperties updateFrom(TeamProperties properties) {
		properties.map.forEach((key, value) -> set(key, value.value));

		return this;
	}

	public <T> T get(TeamProperty<T> key) {
		TeamPropertyValue<T> v = map.get(key);
		return v == null ? key.defaultValue : v.value;
	}

	public <T> void set(TeamProperty<T> key, T value) {
		map.computeIfAbsent(key, TeamPropertyValue::new).value = value;
	}

	public void read(FriendlyByteBuf buffer) {
		int p = buffer.readVarInt();
		map.clear();

		for (int i = 0; i < p; i++) {
			TeamProperty<?> tp = TeamPropertyType.MAP.get(buffer.readUtf(Short.MAX_VALUE)).deserializer.apply(buffer.readResourceLocation(), buffer);
			map.put(tp, new TeamPropertyValue(tp, tp.readValue(buffer)));
		}
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(map.size());

		map.forEach((key, value) -> {
			TeamPropertyType.write(buffer, key);
			key.writeValue(buffer, value.value);
		});
	}

	public void read(CompoundTag tag) {
		tag.getAllKeys().forEach(key -> {
			TeamPropertyValue property = findValue(key);
			if (property != null) {
				Optional<?> optional = property.key.fromNBT(tag.get(key));

				if (optional.isPresent()) {
					property.value = optional.get();
				} else {
					property.value = property.key.defaultValue;
				}
			}
		});
	}

	@Nullable
	private TeamPropertyValue<?> findValue(String key) {
		ResourceLocation id = new ResourceLocation(key);

		return map.entrySet().stream()
				.filter(entry -> entry.getKey().id.equals(id))
				.findFirst()
				.map(Map.Entry::getValue)
				.orElse(null);

	}

	public CompoundTag write(CompoundTag tag) {
		map.forEach((key, value) -> tag.put(key.id.toString(), key.toNBT(value.value)));

		return tag;
	}
}
