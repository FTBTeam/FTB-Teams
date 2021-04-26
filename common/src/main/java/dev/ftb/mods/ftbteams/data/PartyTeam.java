package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbteams.event.PlayerTransferredTeamOwnershipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

public class PartyTeam extends Team {
	UUID owner;

	public PartyTeam(TeamManager m) {
		super(m);
		owner = Util.NIL_UUID;
	}

	@Override
	public TeamType getType() {
		return TeamType.PARTY;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = super.serializeNBT();
		tag.putString("owner", UUIDTypeAdapter.fromUUID(owner));
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		super.deserializeNBT(tag);
		owner = UUIDTypeAdapter.fromString(tag.getString("owner"));
	}

	@Override
	public TeamRank getHighestRank(UUID playerId) {
		if (owner.equals(playerId)) {
			return TeamRank.OWNER;
		}

		return super.getHighestRank(playerId);
	}

	public boolean isOwner(UUID profile) {
		return owner.equals(profile);
	}

	@Override
	public UUID getOwner() {
		return owner;
	}

	@Nullable
	public ServerPlayer getOwnerPlayer() {
		return FTBTUtils.getPlayerByUUID(manager.server, owner);
	}

	@Deprecated
	public int join(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Team oldTeam = manager.getPlayerTeam(player);

		if (oldTeam.getType().isParty()) {
			throw TeamArgument.ALREADY_IN_PARTY.create();
		}

		manager.playerTeamMap.put(id, this);
		ranks.put(id, TeamRank.MEMBER);
		changedTeam(oldTeam, id);
		sendMessage(Util.NIL_UUID, new TextComponent("").append(player.getName()).append(" joined your party!").withStyle(ChatFormatting.GREEN));
		save();

		oldTeam.ranks.remove(id);
		oldTeam.save();
		manager.syncAll();

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int invite(ServerPlayer from, Collection<GameProfile> players) throws CommandSyntaxException {
		for (GameProfile player : players) {
			if (getHighestRank(player.getId()).is(TeamRank.INVITED)) {
				continue;
			}

			ranks.put(player.getId(), TeamRank.INVITED);
			save();

			sendMessage(from.getUUID(), new TextComponent("Invited " + player.getName() + " to ").append(getName()).withStyle(ChatFormatting.GREEN));

			ServerPlayer playerEntity = FTBTUtils.getPlayerByUUID(manager.getServer(), player.getId());

			if (playerEntity != null) {
				MutableComponent component = new TextComponent("You have been invited to ").append(getName()).append("!");
				playerEntity.displayClientMessage(component, false);

				TextComponent accept = new TextComponent("[Click here to accept the invite]");
				accept.setStyle(accept.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ftbteams join " + getStringID())));
				accept.withStyle(ChatFormatting.GREEN);
				playerEntity.displayClientMessage(accept, false);

				TextComponent deny = new TextComponent("[Click here to deny the invite]");
				deny.withStyle(ChatFormatting.RED);
				deny.setStyle(deny.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ftbteams deny_invite " + getStringID())));
				playerEntity.displayClientMessage(deny, false);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int kick(ServerPlayer from, Collection<GameProfile> players) throws CommandSyntaxException {
		for (GameProfile player : players) {
			UUID id = player.getId();
			Team oldTeam = manager.getPlayerTeam(id);

			if (oldTeam != this) {
				throw TeamArgument.NOT_IN_PARTY.create();
			} else if (isOwner(id)) {
				throw TeamArgument.CANT_EDIT.create(getName());
			}

			PlayerTeam team = manager.getInternalPlayerTeam(id);
			manager.playerTeamMap.put(id, team);

			team.ranks.put(id, TeamRank.OWNER);
			team.changedTeam(this, id);
			sendMessage(from.getUUID(), new TextComponent("Kicked ").append(manager.getName(id)).append(" from ").append(getName()).withStyle(ChatFormatting.RED));
			team.save();

			ranks.remove(id);
			save();

			manager.syncAll();

			ServerPlayer playerEntity = FTBTUtils.getPlayerByUUID(manager.getServer(), id);

			if (playerEntity != null) {
				playerEntity.displayClientMessage(new TextComponent("You have been kicked from ").append(getName()).append("!"), false);
				updateCommands(playerEntity);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int transferOwnership(ServerPlayer from, ServerPlayer to) throws CommandSyntaxException {
		if (!getOnlineMembers().contains(to)) {
			throw TeamArgument.NOT_MEMBER.create(to.getDisplayName(), getName());
		}

		ranks.put(owner, TeamRank.OFFICER);
		owner = to.getUUID();
		ranks.put(owner, TeamRank.OWNER);
		save();
		PlayerTransferredTeamOwnershipEvent.EVENT.invoker().accept(new PlayerTransferredTeamOwnershipEvent(this, from, to));

		sendMessage(from.getUUID(), new TextComponent("Transferred ownership to ").append(to.getDisplayName()).withStyle(ChatFormatting.RED));
		updateCommands(from);
		updateCommands(to);
		return Command.SINGLE_SUCCESS;
	}
}
