package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftblibrary.FTBLibraryCommands;
import dev.ftb.mods.ftblibrary.net.EditNBTPacket;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.AbstractTeam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;

import static dev.ftb.mods.ftbteams.command.FTBTeamsCommands.*;

public class NbtEditCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("nbtedit")
                .requires(requiresOPorSP())
                .executes(NbtEditCommand::editPlayerTeamNBT)
                .then(createTeamArg()
                        .executes(NbtEditCommand::editTeamNBT)
                );
    }

    private static int editPlayerTeamNBT(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return edit(ctx, ctx.getSource().getPlayerOrException(), getTeam(ctx));
    }

    private static int editTeamNBT(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return edit(ctx, ctx.getSource().getPlayerOrException(), TeamArgument.get(ctx, "team"));
    }

    private static int edit(CommandContext<CommandSourceStack> ctx, ServerPlayer editor, Team team) {
        if (team instanceof AbstractTeam abstractTeam) {
            CompoundTag info = Util.make(new CompoundTag(), t -> {
                t.store("title", ComponentSerialization.CODEC, abstractTeam.getColoredName());
                t.putString("type", "ftbteams:team");
                t.store("id", UUIDUtil.CODEC, team.getTeamId());
                t.putString("team_type", abstractTeam.getType().getSerializedName());
                t.put("text", FTBLibraryCommands.InfoBuilder.create(ctx)
                        .add("Team Type", Component.translatable(team.getTypeTranslationKey()))
                        .add("Owner", Component.literal(team.getOwner().toString()))
                        .add("Members", Component.literal(String.valueOf(team.getMembers().size())))
                        .build()
                );
            });
            CompoundTag tag = abstractTeam.serializeNBT(ctx.getSource().getServer().registryAccess());
            NetworkHelper.sendTo(editor, new EditNBTPacket(info, tag));
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
}
