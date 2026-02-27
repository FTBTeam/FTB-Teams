package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableConfigValue;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableString;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StringSetProperty extends TeamProperty<Set<String>> {
    public StringSetProperty(Identifier id, Supplier<Set<String>> def) {
        super(id, def);
    }

    public StringSetProperty(Identifier id, Set<String> def) {
        this(id, () -> def);
    }

    static StringSetProperty fromNetwork(Identifier id, FriendlyByteBuf buf) {
        return new StringSetProperty(id, new HashSet<>(buf.readList(b -> b.readUtf(Short.MAX_VALUE))));
    }

    @Override
    public TeamPropertyType<Set<String>> getType() {
        return TeamPropertyType.STRING_SET;
    }

    @Override
    public Optional<Set<String>> fromString(String string) {
        return string.length() > 2 && string.startsWith("[") && string.endsWith("]") ?
                Optional.of(new HashSet<>(Arrays.asList(string.substring(1, string.length() - 1).split("\t")))) :
                Optional.empty();
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeCollection(getDefaultValue(), FriendlyByteBuf::writeUtf);
    }

    @Override
    public String toString(Set<String> value) {
        return "[" + String.join("\t", value) + "]";
    }

    @Override
    public EditableConfigValue<?> config(EditableConfigGroup config, TeamPropertyValue<Set<String>> value) {
        return config.addList(id.getPath(), new ArrayList<>(value.getValue()), new EditableString(), "");
    }

    @Override
    public Tag toNBT(Set<String> value) {
        ListTag res = new ListTag();
        value.forEach(s -> res.add(StringTag.valueOf(s)));
        return res;
    }

    @Override
    public Optional<Set<String>> fromNBT(Tag tag) {
        return tag instanceof ListTag l ?
                Optional.of(l.stream().map(e -> e.asString().orElse("")).collect(Collectors.toSet())) :
                Optional.empty();
    }

    @Override
    public void writeValue(RegistryFriendlyByteBuf buf, Set<String> value) {
        buf.writeCollection(value, FriendlyByteBuf::writeUtf);
    }

    @Override
    public Set<String> readValue(RegistryFriendlyByteBuf buf) {
        return buf.readCollection(HashSet::new, FriendlyByteBuf::readUtf);
    }
}
