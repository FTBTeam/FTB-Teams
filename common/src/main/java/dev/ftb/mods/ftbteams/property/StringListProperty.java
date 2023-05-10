package dev.ftb.mods.ftbteams.property;

import com.google.common.collect.Lists;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class StringListProperty extends TeamProperty<List<String>> {
    public StringListProperty(ResourceLocation id, List<String> def) {
        super(id, def);
    }

    public StringListProperty(ResourceLocation id, FriendlyByteBuf buf) {
        this(id, readList(buf, b -> b.readUtf(Short.MAX_VALUE)));
    }

    @Override
    public TeamPropertyType<List<String>> getType() {
        return TeamPropertyType.STRING_LIST;
    }

    @Override
    public Optional<List<String>> fromString(String string) {
        return string.length() > 2 && string.startsWith("[") && string.endsWith("]") ?
                // the "new ArrayList(...)" part is important here!
                Optional.of(new ArrayList<>(Arrays.asList(string.substring(1, string.length() - 1).split("\t")))) :
                Optional.empty();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        writeCollection(buf, defaultValue, FriendlyByteBuf::writeUtf);
    }

    @Override
    public String toString(List<String> value) {
        return "[" + String.join("\t", value) + "]";
    }

    @Override
    public void config(ConfigGroup config, TeamPropertyValue<List<String>> value) {
        config.addList(id.getPath(), value.value, new StringConfig(), "");
    }

    @Override
    public Tag toNBT(List<String> value) {
        ListTag res = new ListTag();
        value.forEach(s -> res.add(StringTag.valueOf(s)));
        return res;
    }

    @Override
    public Optional<List<String>> fromNBT(Tag tag) {
        List<String> res = new ArrayList<>();
        if (tag instanceof ListTag) {
            ((ListTag) tag).forEach(t -> res.add(t.getAsString()));
            return Optional.of(res);
        } else {
            return Optional.empty();
        }
    }

    // these three methods are in FriendlyByteBuf in 1.18, but not here :(

    public static <T, C extends Collection<T>> C readCollection(FriendlyByteBuf buf, IntFunction<C> intFunction, Function<FriendlyByteBuf, T> function) {
        int i = buf.readVarInt();
        C collection = intFunction.apply(i);

        for(int j = 0; j < i; ++j) {
            collection.add(function.apply(buf));
        }

        return collection;
    }

    public static <T> void writeCollection(FriendlyByteBuf buf, Collection<T> collection, BiConsumer<FriendlyByteBuf, T> biConsumer) {
        buf.writeVarInt(collection.size());

        for (T object : collection) {
            biConsumer.accept(buf, object);
        }

    }

    public static <T> List<T> readList(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> function) {
        return readCollection(buf, Lists::newArrayListWithCapacity, function);
    }
}
