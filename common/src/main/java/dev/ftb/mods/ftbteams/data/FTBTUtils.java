package dev.ftb.mods.ftbteams.data;

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

	private static int rgb(float r, float g, float b) {
		return ((int) r << 16) | ((int) g << 8) | ((int) b);
	}

	public static int randomColor() {
		float hue = MathUtils.RAND.nextFloat();

		float h = (hue - (float) Math.floor(hue)) * 6F;
		float f = h - (float) Math.floor(h);
		float q = 1F - f;
		float t = 1F - (1F - f);

		switch ((int) h) {
			case 0:
				return rgb(255F + 0.5F, t * 255F + 0.5F, 0.5F);
			case 1:
				return rgb(q * 255F + 0.5F, 255F + 0.5F, 0.5F);
			case 2:
				return rgb(0.5F, 255F + 0.5F, t * 255F + 0.5F);
			case 3:
				return rgb(0.5F, q * 255F + 0.5F, 255F + 0.5F);
			case 4:
				return rgb(t * 255F + 0.5F, 0.5F, 255F + 0.5F);
			default:
				return rgb(255F + 0.5F, 0.5F, q * 255F + 0.5F);
		}
	}

	@ExpectPlatform
	public static Component newChatWithLinks(String message) {
		throw new AssertionError();
	}
}
