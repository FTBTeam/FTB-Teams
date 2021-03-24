package dev.ftb.mods.ftbteams.property;

import dev.ftb.mods.ftbteams.event.TeamCollectPropertiesEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TeamProperties {
	public final Map<TeamProperty, TeamPropertyValue> map = new HashMap<>();

	public TeamProperties collect() {
		map.clear();
		TeamCollectPropertiesEvent.EVENT.invoker().accept(new TeamCollectPropertiesEvent(prop -> map.put(prop, new TeamPropertyValue(prop, prop.defaultValue))));
		return this;
	}

	public TeamProperties copyFrom(TeamProperties properties) {
		map.clear();

		for (Map.Entry<TeamProperty, TeamPropertyValue> entry : properties.map.entrySet()) {
			map.put(entry.getKey(), entry.getValue().copy());
		}

		return this;
	}

	public TeamProperties updateFrom(TeamProperties properties) {
		for (Map.Entry<TeamProperty, TeamPropertyValue> entry : properties.map.entrySet()) {
			set(entry.getKey(), entry.getValue().value);
		}

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
			TeamProperty tp = TeamPropertyType.MAP.get(buffer.readUtf(Short.MAX_VALUE)).deserializer.apply(buffer.readResourceLocation(), buffer);
			map.put(tp, new TeamPropertyValue(tp, tp.readValue(buffer)));
		}
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(map.size());

		for (Map.Entry<TeamProperty, TeamPropertyValue> entry : map.entrySet()) {
			TeamPropertyType.write(buffer, entry.getKey());
			entry.getKey().writeValue(buffer, entry.getValue().value);
		}
	}

	public void read(CompoundTag tag) {
		for (String key : tag.getAllKeys()) {
			TeamPropertyValue property = findValue(key);

			if (property != null) {
				Optional optional = property.key.fromNBT(tag.get(key));

				if (optional.isPresent()) {
					property.value = optional.get();
				} else {
					property.value = property.key.defaultValue;
				}
			}
		}
	}

	@Nullable
	private TeamPropertyValue findValue(String key) {
		ResourceLocation id = new ResourceLocation(key);

		for (Map.Entry<TeamProperty, TeamPropertyValue> entry : map.entrySet()) {
			if (entry.getKey().id.equals(id)) {
				return entry.getValue();
			}
		}

		return null;
	}

	public CompoundTag write(CompoundTag tag) {
		for (Map.Entry<TeamProperty, TeamPropertyValue> entry : map.entrySet()) {
			tag.put(entry.getKey().id.toString(), entry.getKey().toNBT(entry.getValue().value));
		}

		return tag;
	}
}
