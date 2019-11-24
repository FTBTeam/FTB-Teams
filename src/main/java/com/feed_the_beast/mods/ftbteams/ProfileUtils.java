package com.feed_the_beast.mods.ftbteams;

import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ProfileUtils
{
	public static final GameProfile NO_PROFILE = new GameProfile(new UUID(0L, 0L), "-");

	@Nullable
	public static ServerPlayerEntity getPlayerByProfile(MinecraftServer server, GameProfile profile)
	{
		ServerPlayerEntity playerEntity = profile.getId() == null ? null : server.getPlayerList().getPlayerByUUID(profile.getId());
		return playerEntity != null ? playerEntity : profile.getName() != null ? server.getPlayerList().getPlayerByUsername(profile.getName()) : null;
	}

	public static GameProfile normalize(@Nullable GameProfile profile)
	{
		if (profile == null || profile.getId() == null || profile.getName() == null || profile.equals(NO_PROFILE))
		{
			return NO_PROFILE;
		}

		return profile;
	}

	public static String serializeProfile(@Nullable GameProfile profile)
	{
		if (normalize(profile) == NO_PROFILE)
		{
			return "";
		}

		return UUIDTypeAdapter.fromUUID(profile.getId()) + ":" + profile.getName();
	}

	public static GameProfile deserializeProfile(String string)
	{
		if (string.isEmpty())
		{
			return NO_PROFILE;
		}

		try
		{
			String[] s = string.split(":", 2);
			UUID uuid = UUIDTypeAdapter.fromString(s[0]);
			String name = s[1];
			return normalize(new GameProfile(uuid, name));
		}
		catch (Exception ex)
		{
			return NO_PROFILE;
		}
	}
}