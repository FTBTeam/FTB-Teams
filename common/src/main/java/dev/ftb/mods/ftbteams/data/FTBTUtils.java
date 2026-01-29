package dev.ftb.mods.ftbteams.data;

import com.mojang.brigadier.tree.CommandNode;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FTBTUtils {
	@Nullable
	public static ServerPlayer getPlayerByUUID(MinecraftServer server, @Nullable UUID id) {
		return id == null || id == Util.NIL_UUID ? null : server.getPlayerList().getPlayer(id);
	}

	public static Color4I randomColor() {
		return Color4I.hsb(MathUtils.RAND.nextFloat(), 0.65F, 1F);
	}

	public static boolean canPlayerUseCommand(ServerPlayer player, String command) {
		List<String> parts = Arrays.asList(command.split("\\."));
		CommandNode<CommandSourceStack> node = player.level().getServer().getCommands().getDispatcher().findNode(parts);
		return node != null && node.canUse(player.createCommandSourceStack());
	}

	public static String getDefaultPartyName(MinecraftServer server, UUID playerId, @Nullable ServerPlayer player) {
		String playerName;
		if (player != null) {
			playerName = player.getGameProfile().name();
		} else {
			Optional<NameAndId> profileCache =  server.services().nameToIdCache().get(playerId);
            playerName = profileCache.map(NameAndId::name).orElse(playerId.toString());
		}
		return playerName + "'s Party";
	}

	static MutableComponent makeCopyableComponent(String id) {
		return Component.literal(id)
				.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click"))))
				.withStyle(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(id)));
	}
}
