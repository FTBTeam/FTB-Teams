package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.event.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
	protected void serializeExtraNBT(CompoundTag tag) {
		tag.putString("owner", owner.toString());
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		super.deserializeNBT(tag);
		owner = UUID.fromString(tag.getString("owner"));
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

	public int join(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Team oldTeam = manager.getPlayerTeam(player);

		if (!oldTeam.getType().isPlayer()) {
			throw TeamArgument.ALREADY_IN_PARTY.create();
		}

		UUID id = player.getUUID();

		((PlayerTeam) oldTeam).actualTeam = this;
		ranks.put(id, TeamRank.MEMBER);
		sendMessage(Util.NIL_UUID, Component.translatable("ftbteams.message.joined", player.getName()).withStyle(ChatFormatting.GREEN));
		save();

		oldTeam.ranks.remove(id);
		oldTeam.save();
		((PlayerTeam) oldTeam).updatePresence();
		manager.syncTeamsToAll(this, oldTeam);
		changedTeam(oldTeam, id, player, false);
		return Command.SINGLE_SUCCESS;
	}

	public int invite(ServerPlayer from, Collection<GameProfile> players) throws CommandSyntaxException {
		if (!FTBTUtils.canPlayerUseCommand(from, "ftbteams.party.invite")) {
			throw TeamArgument.NO_PERMISSION.create();
		}

		for (GameProfile player : players) {
			if (FTBTeamsAPI.getManager().getPlayerTeam(player.getId()) instanceof PartyTeam) {
				throw TeamArgument.PLAYER_IN_PARTY.create(player.getName());
			}

			ranks.put(player.getId(), TeamRank.INVITED);
			TeamEvent.INVITED.invoker().accept(new TeamInvitedEvent(this, players, from));
			save();


			sendMessage(from.getUUID(), Component.translatable("ftbteams.message.invited", Component.literal(player.getName()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));

			ServerPlayer p = FTBTUtils.getPlayerByUUID(manager.getServer(), player.getId());

			if (p != null) {
				p.displayClientMessage(Component.translatable("ftbteams.message.invite_sent", from.getName().copy().withStyle(ChatFormatting.YELLOW)), false);
				Component acceptButton = Component.translatable("ftbteams.accept")
						.withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(
								new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party join " + getStringID()))
						);
				Component declineButton = Component.translatable("ftbteams.decline")
						.withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(
								new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party deny_invite " + getStringID()))
						);
				p.displayClientMessage(Component.literal("[").append(acceptButton).append("] [").append(declineButton).append("]"), false);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	public int kick(CommandSourceStack from, Collection<GameProfile> players) throws CommandSyntaxException {
		for (GameProfile player : players) {
			UUID id = player.getId();
			Team oldTeam = manager.getPlayerTeam(id);

			if (oldTeam != this) {
				throw TeamArgument.NOT_IN_PARTY.create();
			} else if (isOwner(id)) {
				throw TeamArgument.CANT_KICK_OWNER.create();
			}

			PlayerTeam team = manager.getInternalPlayerTeam(id);
			team.actualTeam = team;

			ServerPlayer playerEntity = FTBTUtils.getPlayerByUUID(manager.getServer(), id);

			team.ranks.put(id, TeamRank.OWNER);
			UUID fromId = from.getPlayer() != null ? from.getPlayer().getUUID() : Util.NIL_UUID;
			sendMessage(fromId, Component.translatable("ftbteams.message.kicked", manager.getName(id).copy().withStyle(ChatFormatting.YELLOW), getName()).withStyle(ChatFormatting.GOLD));
			team.save();

			ranks.remove(id);
			save();

			team.updatePresence();
			manager.syncTeamsToAll(this, team);

			if (playerEntity != null) {
				playerEntity.displayClientMessage(Component.translatable("ftbteams.message.kicked", playerEntity.getName().copy().withStyle(ChatFormatting.YELLOW), getName().copy().withStyle(ChatFormatting.AQUA)), false);
				updateCommands(playerEntity);
			}

			team.changedTeam(this, id, playerEntity, false);
		}

		return Command.SINGLE_SUCCESS;
	}

	public int promote(ServerPlayer from, Collection<GameProfile> players) throws CommandSyntaxException {
		boolean changesMade = false;
		for (GameProfile player : players) {
			UUID id = player.getId();
			if (getHighestRank(id) == TeamRank.MEMBER) {
				ranks.put(id, TeamRank.OFFICER);
				TeamEvent.RANK_CHANGED.invoker().accept(new TeamRankChangedEvent(this, player, TeamRank.MEMBER, TeamRank.OFFICER));
				sendMessage(from.getUUID(), Component.translatable("ftbteams.message.promoted", manager.getName(id).copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
				changesMade = true;
			} else {
				throw TeamArgument.NOT_MEMBER.create(manager.getName(id), getName());
			}
		}
		if (changesMade) {
			save();
			manager.syncTeamsToAll(this);
		}
		return Command.SINGLE_SUCCESS;
	}

	public int demote(ServerPlayer from, Collection<GameProfile> players) throws CommandSyntaxException {
		boolean changesMade = false;
		for (GameProfile player : players) {
			UUID id = player.getId();
			if (getHighestRank(id) == TeamRank.OFFICER) {
				ranks.put(id, TeamRank.MEMBER);
				TeamEvent.RANK_CHANGED.invoker().accept(new TeamRankChangedEvent(this, player, TeamRank.OFFICER, TeamRank.MEMBER));
				sendMessage(from.getUUID(), Component.translatable("ftbteams.message.demoted", manager.getName(id).copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
				changesMade = true;
			} else {
				throw TeamArgument.NOT_OFFICER.create(manager.getName(id), getName());
			}
		}
		if (changesMade) {
			manager.syncTeamsToAll(this);
		}
		return Command.SINGLE_SUCCESS;
	}

	public int transferOwnership(CommandSourceStack from, Collection<GameProfile> toProfiles) throws CommandSyntaxException {
		return transferOwnership(from, toProfiles.stream().findFirst().orElseThrow());
	}

	public int transferOwnership(CommandSourceStack from, GameProfile toProfile) throws CommandSyntaxException {
		// new owner must be in this party
		UUID newOwnerID = toProfile.getId();
		if (!getMembers().contains(newOwnerID)) {
			throw TeamArgument.NOT_MEMBER.create(toProfile.toString(), getName());
		}

		if (getHighestRank(newOwnerID) == TeamRank.OWNER) {
			from.sendSystemMessage(Component.literal("Already owner!").withStyle(ChatFormatting.RED));
			return 0;
		}

		ranks.put(owner, TeamRank.OFFICER);
		owner = newOwnerID;
		ranks.put(owner, TeamRank.OWNER);
		save();

		ServerPlayer fromPlayer = from.getPlayer();  // null if command run from console
		if (fromPlayer != null) {
			updateCommands(fromPlayer);
		}

		ServerPlayer toPlayer = from.getServer().getPlayerList().getPlayer(newOwnerID);
		if (toPlayer != null) {
			TeamEvent.OWNERSHIP_TRANSFERRED.invoker().accept(new PlayerTransferredTeamOwnershipEvent(this, fromPlayer, toPlayer));
			updateCommands(toPlayer);
		} else {
			TeamEvent.OWNERSHIP_TRANSFERRED.invoker().accept(new PlayerTransferredTeamOwnershipEvent(this, fromPlayer, toProfile));
		}

		UUID fromId = fromPlayer == null ? Util.NIL_UUID : fromPlayer.getUUID();
		Component msg = Component.translatable("ftbteams.message.transfer_owner", Component.literal(toProfile.getName()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN);
		sendMessage(fromId, msg);
		if (fromPlayer == null) {
			from.sendSystemMessage(msg);
		}

		manager.syncTeamsToAll(this);

		return Command.SINGLE_SUCCESS;
	}

	public int leave(UUID id) throws CommandSyntaxException {
		ServerPlayer player = FTBTeamsAPI.getManager().getServer().getPlayerList().getPlayer(id);

		if (isOwner(id) && getMembers().size() > 1) {
			throw TeamArgument.OWNER_CANT_LEAVE.create();
		}

		PlayerTeam team = manager.getInternalPlayerTeam(id);
		team.actualTeam = team;

		team.ranks.put(id, TeamRank.OWNER);
		MutableComponent playerName = player == null ? Component.literal(id.toString()) : player.getName().copy();
		sendMessage(Util.NIL_UUID, Component.translatable("ftbteams.message.left_party", playerName.withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
		team.save();

		ranks.remove(id);
		manager.save();
		boolean deleted = false;

		if (getMembers().isEmpty()) {
			deleted = true;
			manager.saveNow();
			manager.teamMap.remove(getId());
			String fn = getId() + ".snbt";

			try {
				Path dir = manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("deleted");

				if (Files.notExists(dir)) {
					Files.createDirectories(dir);
				}

				Files.move(manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("party/" + fn), dir.resolve(fn));
			} catch (IOException e) {
				e.printStackTrace();

				try {
					Files.deleteIfExists(manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("party/" + fn));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		team.updatePresence();
		manager.syncTeamsToAll(this, team);
		team.changedTeam(this, id, player, deleted);
		return Command.SINGLE_SUCCESS;
	}

	public int addAlly(CommandSourceStack source, Collection<GameProfile> players) throws CommandSyntaxException {
		if (source.getPlayer() != null && !FTBTUtils.canPlayerUseCommand(source.getPlayer(), "ftbteams.party.allies.add")) {
			throw TeamArgument.NO_PERMISSION.create();
		}

		UUID from = source.getEntity() == null ? Util.NIL_UUID : source.getEntity().getUUID();

		List<GameProfile> addedPlayers = new ArrayList<>();
		for (GameProfile player : players) {
			UUID id = player.getId();

			if (!isAlly(id)) {
				ranks.put(id, TeamRank.ALLY);
				sendMessage(from, Component.translatable("ftbteams.message.add_ally",
						manager.getName(id).copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
				addedPlayers.add(player);
				ServerPlayer invitedPlayer = manager.getServer().getPlayerList().getPlayer(id);
				if (invitedPlayer != null) {
					invitedPlayer.displayClientMessage(Component.translatable("ftbteams.message.now_allied", getDisplayName()).withStyle(ChatFormatting.GREEN), false);
				}
			}
		}

		if (!addedPlayers.isEmpty()) {
			save();
			manager.syncTeamsToAll(this);
			TeamEvent.ADD_ALLY.invoker().accept(new TeamAllyEvent(this, addedPlayers, true));
			return 1;
		}

		return 0;
	}

	public int removeAlly(CommandSourceStack source, Collection<GameProfile> players) throws CommandSyntaxException {
		UUID from = source.getEntity() == null ? Util.NIL_UUID : source.getEntity().getUUID();
		List<GameProfile> removedPlayers = new ArrayList<>();

		for (GameProfile player : players) {
			UUID id = player.getId();

			if (isAlly(id) && !isMember(id)) {
				ranks.remove(id);
				sendMessage(from, Component.translatable("ftbteams.message.remove_ally",
						manager.getName(id).copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
				removedPlayers.add(player);
				ServerPlayer removedPlayer = manager.getServer().getPlayerList().getPlayer(id);
				if (removedPlayer != null) {
					removedPlayer.displayClientMessage(Component.translatable("ftbteams.message.no_longer_allied", getDisplayName()).withStyle(ChatFormatting.GOLD), false);
				}
			}
		}

		if (!removedPlayers.isEmpty()) {
			save();
			manager.syncTeamsToAll(this);
			TeamEvent.REMOVE_ALLY.invoker().accept(new TeamAllyEvent(this, removedPlayers, false));
			return 1;
		}

		return 0;
	}

	public int listAllies(CommandSourceStack source) throws CommandSyntaxException {
		source.sendSuccess(Component.literal("Allies:"), false);
		boolean any = false;

		for (Map.Entry<UUID, TeamRank> entry : getRanked(TeamRank.ALLY).entrySet()) {
			if (!entry.getValue().is(TeamRank.MEMBER)) {
				source.sendSuccess(manager.getName(entry.getKey()), false);
				any = true;
			}
		}

		if (!any) {
			source.sendSuccess(Component.literal("None"), false);
		}

		return 1;
	}

	public int forceDisband(CommandSourceStack from) throws CommandSyntaxException {
		// kick all non-owner members
		Set<UUID> members = new HashSet<>(getMembers());
		members.remove(owner);
		kick(from, members.stream().map(id -> new GameProfile(id, null)).toList());

		// now make the owner leave too
		leave(owner);

		from.sendSuccess(Component.translatable("ftbteams.message.team_disbanded", getName(), getId()).withStyle(ChatFormatting.GOLD), false);

		return 1;
	}
}
