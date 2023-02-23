package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftbteams.event.TeamCollectPropertiesEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

public class TeamProperties {
	private final Map<TeamProperty, TeamPropertyValue> map = new LinkedHashMap<>();

	public static TeamProperties fromNetwork(FriendlyByteBuf buf) {
		TeamProperties properties = new TeamProperties();
		properties.read(buf);
		return properties;
	}

	public void forEach(BiConsumer<TeamProperty,TeamPropertyValue> consumer) {
		map.forEach(consumer);
	}

	public void collect() {
		map.clear();
		TeamEvent.COLLECT_PROPERTIES.invoker().accept(new TeamCollectPropertiesEvent(prop -> map.put(prop, new TeamPropertyValue(prop, prop.defaultValue))));
	}

	public TeamProperties copy() {
		TeamProperties p = new TeamProperties();

		map.forEach((key, value) -> p.map.put(key, value.copy()));

		return p;
	}

	public void updateFrom(TeamProperties properties) {
		properties.map.forEach((key, value) -> set(key, value.value));
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
			TeamProperty<?> tp = TeamPropertyType.read(buffer);
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

	public void writeSyncableOnly(FriendlyByteBuf buffer, List<TeamProperty> syncableProps) {
		// this is used when sync'ing team data for a different team
		// player A only needs to know limited info (display name, color...) about team B if A isn't a member of B
		Map<TeamProperty, TeamPropertyValue> subMap = new HashMap<>();
		syncableProps.forEach(prop -> {
			if (map.containsKey(prop)) {
				subMap.put(prop, map.get(prop));
			}
		});

		buffer.writeVarInt(subMap.size());
		subMap.forEach((key, value) -> {
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
