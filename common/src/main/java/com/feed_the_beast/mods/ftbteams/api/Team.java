package com.feed_the_beast.mods.ftbteams.api;

import com.feed_the_beast.mods.ftbteams.impl.TeamManagerImpl;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * @author LatvianModder
 */
public interface Team
{
	TeamManagerImpl getManager();

	void save();

	int getId();

	String getStringID();

	Component getName();

	boolean isServerTeam();

	<T> T getProperty(TeamProperty<T> property);

	<T> void setProperty(TeamProperty<T> property, T value);

	boolean create();

	boolean delete();

	boolean isOwner(GameProfile profile);

	default boolean isOwner(ServerPlayer player)
	{
		return isOwner(player.getGameProfile());
	}

	GameProfile getOwner();

	@Nullable
	ServerPlayer getOwnerPlayer();

	boolean isMember(GameProfile profile);

	default boolean isMember(ServerPlayer player)
	{
		return isMember(player.getGameProfile());
	}

	Set<GameProfile> getMembers();

	List<ServerPlayer> getOnlineMembers();

	boolean addMember(ServerPlayer player);

	boolean removeMember(GameProfile profile, boolean deleteWhenEmpty);

	boolean isAlly(GameProfile profile);

	default boolean isAlly(ServerPlayer player)
	{
		return isAlly(player.getGameProfile());
	}

	Set<GameProfile> getAllies();

	boolean isInvited(GameProfile profile);

	default boolean isInvited(ServerPlayer player)
	{
		return isInvited(player.getGameProfile());
	}

	Set<GameProfile> getInvited();
}