package com.feed_the_beast.mods.ftbteams.api;

import com.feed_the_beast.mods.ftbteams.impl.TeamManagerImpl;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;

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

	ITextComponent getName();

	boolean isServerTeam();

	<T> T getProperty(TeamProperty<T> property);

	<T> void setProperty(TeamProperty<T> property, T value);

	boolean create();

	boolean delete();

	boolean isOwner(GameProfile profile);

	default boolean isOwner(ServerPlayerEntity player)
	{
		return isOwner(player.getGameProfile());
	}

	GameProfile getOwner();

	@Nullable
	ServerPlayerEntity getOwnerPlayer();

	boolean isMember(GameProfile profile);

	default boolean isMember(ServerPlayerEntity player)
	{
		return isMember(player.getGameProfile());
	}

	Set<GameProfile> getMembers();

	List<ServerPlayerEntity> getOnlineMembers();

	boolean addMember(ServerPlayerEntity player);

	boolean removeMember(GameProfile profile, boolean deleteWhenEmpty);

	boolean isAlly(GameProfile profile);

	default boolean isAlly(ServerPlayerEntity player)
	{
		return isAlly(player.getGameProfile());
	}

	Set<GameProfile> getAllies();

	boolean isInvited(GameProfile profile);

	default boolean isInvited(ServerPlayerEntity player)
	{
		return isInvited(player.getGameProfile());
	}

	Set<GameProfile> getInvited();
}