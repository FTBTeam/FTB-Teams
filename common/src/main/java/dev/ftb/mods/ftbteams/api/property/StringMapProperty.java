package dev.ftb.mods.ftbteams.api.property;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class StringMapProperty<T> extends TeamProperty<Map<String,T>> {
    private final TeamPropertyType<Map<String, T>> propType;
    private final Function<String,T> fromString;
    private final BiConsumer<FriendlyByteBuf, T> toNet;
    private final Function<FriendlyByteBuf, T> fromNet;

    protected StringMapProperty(Identifier id, Supplier<Map<String, T>> defaultValue, TeamPropertyType<Map<String, T>> propType,
                                Function<String,T> fromString, BiConsumer<FriendlyByteBuf,T> toNet, Function<FriendlyByteBuf,T> fromNet)
    {
        super(id, defaultValue);
        this.propType = propType;
        this.fromString = fromString;
        this.toNet = toNet;
        this.fromNet = fromNet;
    }

    @Override
    public TeamPropertyType<Map<String, T>> getType() {
        return propType;
    }

    @Override
    public Optional<Map<String, T>> fromString(String string) {
        try {
            Map<String,T> res = new HashMap<>();
            Splitter.on(",").withKeyValueSeparator("=").split(string)
                    .forEach((k, v) -> res.put(k, fromString.apply(v)));
            return Optional.of(res);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString(Map<String, T> value) {
        return Joiner.on(",").withKeyValueSeparator("=").join(value);
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeMap(getDefaultValue(), FriendlyByteBuf::writeUtf, toNet::accept);
    }

    @Override
    public Tag toNBT(Map<String, T> value) {
        CompoundTag res = new CompoundTag();
        value.forEach((k, v) -> res.putString(k, v.toString()));
        return res;
    }

    @Override
    public Optional<Map<String, T>> fromNBT(Tag tag) {
        if (tag instanceof CompoundTag c) {
            Map<String,T> res = new HashMap<>();
            c.keySet().forEach(k -> {
                res.put(k, fromString.apply(c.getString(k).orElse(null)));
            });
            return Optional.of(res);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, T> readValue(RegistryFriendlyByteBuf buf) {
        return buf.readMap(FriendlyByteBuf::readUtf, fromNet::apply);
    }

    @Override
    public void writeValue(RegistryFriendlyByteBuf buf, Map<String, T> value) {
        buf.writeMap(value, FriendlyByteBuf::writeUtf, toNet::accept);
    }

    protected static <T> Map<String,T> mapFromNetwork(FriendlyByteBuf buf, Function<FriendlyByteBuf,T> fromNet) {
        return buf.readMap(FriendlyByteBuf::readUtf, fromNet::apply);
    }

    public static class ToInteger extends StringMapProperty<Integer> {
        public ToInteger(Identifier id, Map<String, Integer> defaultValue) {
            super(id, () -> defaultValue, TeamPropertyType.INT_MAP, Integer::parseInt, FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt);
        }

        static ToInteger fromNetwork(Identifier id, FriendlyByteBuf buf) {
            return new ToInteger(id, mapFromNetwork(buf, FriendlyByteBuf::readVarInt));
        }
    }

    public static class ToBoolean extends StringMapProperty<Boolean> {
        public ToBoolean(Identifier id, Map<String, Boolean> defaultValue) {
            super(id, () -> defaultValue, TeamPropertyType.BOOL_MAP, Boolean::parseBoolean, FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean);
        }

        static ToBoolean fromNetwork(Identifier id, FriendlyByteBuf buf) {
            return new ToBoolean(id, mapFromNetwork(buf, FriendlyByteBuf::readBoolean));
        }
    }

    public static class ToString extends StringMapProperty<String> {
        public ToString(Identifier id, Map<String, String> defaultValue) {
            super(id, () -> defaultValue, TeamPropertyType.STRING_MAP, Function.identity(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf);
        }

        static ToString fromNetwork(Identifier id, FriendlyByteBuf buf) {
            return new ToString(id, mapFromNetwork(buf, FriendlyByteBuf::readUtf));
        }
    }
}
