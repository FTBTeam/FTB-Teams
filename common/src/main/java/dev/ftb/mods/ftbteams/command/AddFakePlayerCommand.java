package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;

public class AddFakePlayerCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("add_fake_player")
                .requires(cs -> Platform.isDevelopmentEnvironment() && cs.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("profile", GameProfileArgument.gameProfile())
                        .executes(ctx -> addFakePlayer(GameProfileArgument.getGameProfiles(ctx, "profile")))
                );
    }

    private static int addFakePlayer(Collection<NameAndId> profiles) {
        if (TeamManagerImpl.INSTANCE != null) {
            for (NameAndId profile : profiles) {
                TeamManagerImpl.INSTANCE.playerLoggedIn(null, profile.id(), profile.name());
            }
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
}
