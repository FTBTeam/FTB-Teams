package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableConfigValue;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableStringifiedConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BigIntegerProperty extends TeamProperty<BigInteger> {
    public BigIntegerProperty(Identifier id, Supplier<BigInteger> def) {
        super(id, def);
    }

    public BigIntegerProperty(Identifier id, BigInteger def) {
        this(id, () -> def);
    }

    @Override
    public TeamPropertyType<BigInteger> getType() {
        return TeamPropertyType.BIG_INTEGER;
    }

    @Override
    public Optional<BigInteger> fromString(String string) {
        try {
            return Optional.of(new BigInteger(string));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeByteArray(getDefaultValue().toByteArray());
    }

    public static TeamProperty<BigInteger> fromNetwork(Identifier id, RegistryFriendlyByteBuf buf) {
        return new BigIntegerProperty(id, new BigInteger(buf.readByteArray()));
    }

    @Override
    public BigInteger readValue(RegistryFriendlyByteBuf buf) {
        return new BigInteger(buf.readByteArray());
    }

    @Override
    public void writeValue(RegistryFriendlyByteBuf buf, BigInteger value) {
        buf.writeByteArray(value.toByteArray());
    }

    @Override
    public EditableConfigValue<?> config(EditableConfigGroup config, TeamPropertyValue<BigInteger> value) {
        return config.add(id.getPath(), new BigIntegerConfig(), value.getValue(), value::setValue, getDefaultValue());
    }

    private static class BigIntegerConfig extends EditableStringifiedConfig<BigInteger> {
        @Override
        public boolean parse(@Nullable Consumer<BigInteger> consumer, String s) {
            try {
                BigInteger b = new BigInteger(s);
                return okValue(consumer, b);
            } catch (NumberFormatException ignored) {
            }
            return false;
        }
    }
}
