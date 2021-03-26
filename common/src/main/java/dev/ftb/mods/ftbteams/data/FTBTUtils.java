package dev.ftb.mods.ftbteams.data;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import me.shedaniel.architectury.annotations.ExpectPlatform;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

public class FTBTUtils {
	public static final GameProfile NO_PROFILE = new GameProfile(new UUID(0L, 0L), "-");

	@Nullable
	public static ServerPlayer getPlayerByUUID(MinecraftServer server, @Nullable UUID id) {
		return id == null || id == Util.NIL_UUID ? null : server.getPlayerList().getPlayer(id);
	}

	public static GameProfile normalize(@Nullable GameProfile profile) {
		if (profile == null || profile.getId() == null || profile.getName() == null || profile.equals(NO_PROFILE)) {
			return NO_PROFILE;
		}

		if (!profile.getProperties().isEmpty()) {
			return new GameProfile(profile.getId(), profile.getName());
		}

		return profile;
	}

	public static String serializeProfile(@Nullable GameProfile profile) {
		if (normalize(profile) == NO_PROFILE) {
			return "";
		}

		return UUIDTypeAdapter.fromUUID(profile.getId()) + ":" + profile.getName();
	}

	public static GameProfile deserializeProfile(String string) {
		if (string.isEmpty()) {
			return NO_PROFILE;
		}

		try {
			String[] s = string.split(":", 2);
			UUID uuid = UUIDTypeAdapter.fromString(s[0]);
			String name = s[1];
			return normalize(new GameProfile(uuid, name));
		} catch (Exception ex) {
			return NO_PROFILE;
		}
	}

	public static Color4I randomColor() {
		return Color4I.hsb(MathUtils.RAND.nextFloat(), 0.65F, 1F);
	}

	@ExpectPlatform
	public static Component newChatWithLinks(String message) {
		throw new AssertionError();
	}
}
