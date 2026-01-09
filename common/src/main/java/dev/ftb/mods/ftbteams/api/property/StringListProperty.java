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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class StringListProperty extends TeamProperty<List<String>> {
    public StringListProperty(Identifier id, Supplier<List<String>> def) {
        super(id, def);
    }

    public StringListProperty(Identifier id, List<String> def) {
        this(id, () -> def);
    }

    static StringListProperty fromNetwork(Identifier id, FriendlyByteBuf buf) {
        return new StringListProperty(id, buf.readList(b -> b.readUtf(Short.MAX_VALUE)));
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
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeCollection(getDefaultValue(), FriendlyByteBuf::writeUtf);
    }

    @Override
    public String toString(List<String> value) {
        return "[" + String.join("\t", value) + "]";
    }

    @Override
    public EditableConfigValue<?> config(EditableConfigGroup config, TeamPropertyValue<List<String>> value) {
        return config.addList(id.getPath(), value.getValue(), new EditableString(), "");
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
        if (tag instanceof ListTag l) {
            l.forEach(t -> res.add(t.asString().orElse("")));
            return Optional.of(res);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void writeValue(RegistryFriendlyByteBuf buf, List<String> value) {
        buf.writeCollection(value, FriendlyByteBuf::writeUtf);
    }

    @Override
    public List<String> readValue(RegistryFriendlyByteBuf buf) {
        return buf.readList(FriendlyByteBuf::readUtf);
    }
}
