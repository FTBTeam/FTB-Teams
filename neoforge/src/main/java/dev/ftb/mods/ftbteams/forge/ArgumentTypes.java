package dev.ftb.mods.ftbteams.forge;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyArgument;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES
            = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, FTBTeamsAPI.MOD_ID);

    public static final DeferredHolder<ArgumentTypeInfo<?,?>, TeamArgument.Info> TEAM_ARGUMENT
            = COMMAND_ARGUMENT_TYPES.register("team", () -> ArgumentTypeInfos.registerByClass(TeamArgument.class, new TeamArgument.Info()));
    public static final DeferredHolder<ArgumentTypeInfo<?,?>, TeamPropertyArgument.Info> TEAM_PROPERTY_ARGUMENT
            = COMMAND_ARGUMENT_TYPES.register("team_property", () -> ArgumentTypeInfos.registerByClass(TeamPropertyArgument.class, new TeamPropertyArgument.Info()));
}
