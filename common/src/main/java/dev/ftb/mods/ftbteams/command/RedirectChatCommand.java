package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RedirectChatCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("redirect_chat")
                .executes(RedirectChatCommand::redirectChatToggle);
    }

    private static int redirectChatToggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        TeamManager mgr = FTBTeamsAPI.api().getManager();
        mgr.setChatRedirected(player, !mgr.isChatRedirected(player));
        String key = "ftbteams.message.chat_redirected." + (mgr.isChatRedirected(player) ? "on" : "off");
        ctx.getSource().sendSuccess(() -> Component.translatable(key).withStyle(ChatFormatting.ITALIC, ChatFormatting.GOLD), false);
        return Command.SINGLE_SUCCESS;
    }
}
