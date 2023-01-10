package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.event.PlayerTransferredTeamOwnershipEvent;
import dev.ftb.mods.ftbteams.event.TeamAllyEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
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
		sendMessage(Util.NIL_UUID, new TextComponent("").append(player.getName()).append(" joined your party!").withStyle(ChatFormatting.GREEN));
		save();

		oldTeam.ranks.remove(id);
		oldTeam.save();
		((PlayerTeam) oldTeam).updatePresence();
		manager.syncAll();
		changedTeam(oldTeam, id, player, false);
		return Command.SINGLE_SUCCESS;
	}

	public int invite(ServerPlayer from, Collection<GameProfile> players) throws CommandSyntaxException {
		for (GameProfile player : players) {
			if (FTBTeamsAPI.getManager().getPlayerTeam(player.getId()) instanceof PartyTeam) {
				throw TeamArgument.PLAYER_IN_PARTY.create(player.getName());
			}

			ranks.put(player.getId(), TeamRank.INVITED);
			save();

			sendMessage(from.getUUID(), new TranslatableComponent("ftbteams.message.invited", new TextComponent(player.getName()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));

			ServerPlayer p = FTBTUtils.getPlayerByUUID(manager.getServer(), player.getId());

			if (p != null) {
				p.sendMessage(new TranslatableComponent("ftbteams.message.invite_sent", from.getName().copy().withStyle(ChatFormatting.YELLOW)), Util.NIL_UUID);
				Component acceptButton = new TranslatableComponent("ftbteams.accept")
						.withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(
								new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party join " + getStringID()))
						);
				Component declineButton = new TranslatableComponent("ftbteams.decline")
						.withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(
								new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party deny_invite " + getStringID()))
						);
				p.sendMessage(new TextComponent("[").append(acceptButton).append("] [").append(declineButton).append("]"), Util.NIL_UUID);
			}
		}

		return Command.SINGLE_SUCCESS;
	}

	public int kick(ServerPlayer from, Collection<GameProfile> players) throws CommandSyntaxException {
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
			sendMessage(from.getUUID(), new TranslatableComponent("ftbteams.message.kicked", manager.getName(id).copy().withStyle(ChatFormatting.YELLOW), getName()).withStyle(ChatFormatting.GOLD));
			team.save();

			ranks.remove(id);
			save();

			team.updatePresence();
			manager.syncAll();

			if (playerEntity != null) {
				playerEntity.displayClientMessage(new TranslatableComponent("ftbteams.message.kicked", playerEntity.getName().copy().withStyle(ChatFormatting.YELLOW), getName().copy().withStyle(ChatFormatting.AQUA)), false);
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
				sendMessage(from.getUUID(), new TranslatableComponent("ftbteams.message.promoted", manager.getName(id).copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
				changesMade = true;
			} else {
				throw TeamArgument.NOT_MEMBER.create(manager.getName(id), getName());
			}
		}
		if (changesMade) {
			save();
			manager.syncAll();
		}
		return Command.SINGLE_SUCCESS;
	}

	public int demote(ServerPlayer from, Collection<GameProfile> players) throws CommandSyntaxException {
		boolean changesMade = false;
		for (GameProfile player : players) {
			UUID id = player.getId();
			if (getHighestRank(id) == TeamRank.OFFICER) {
				ranks.put(id, TeamRank.MEMBER);
				sendMessage(from.getUUID(), new TranslatableComponent("ftbteams.message.demoted", manager.getName(id).copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
				changesMade = true;
			} else {
				throw TeamArgument.NOT_OFFICER.create(manager.getName(id), getName());
			}
		}
		if (changesMade) {
			manager.syncAll();
		}
		return Command.SINGLE_SUCCESS;
	}

	public int transferOwnership(ServerPlayer from, ServerPlayer to) throws CommandSyntaxException {
		if (!getOnlineMembers().contains(to)) {
			throw TeamArgument.NOT_MEMBER.create(to.getDisplayName(), getName());
		}

		if (from == to) {
			from.sendMessage(new TextComponent("What."), Util.NIL_UUID);
			return 0;
		}

		ranks.put(owner, TeamRank.OFFICER);
		owner = to.getUUID();
		ranks.put(owner, TeamRank.OWNER);
		save();
		TeamEvent.OWNERSHIP_TRANSFERRED.invoker().accept(new PlayerTransferredTeamOwnershipEvent(this, from, to));

		sendMessage(from.getUUID(), new TranslatableComponent("ftbteams.message.transfer_owner", to.getDisplayName().copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
		updateCommands(from);
		updateCommands(to);
		manager.syncAll();
		return Command.SINGLE_SUCCESS;
	}

	public int leave(ServerPlayer player) throws CommandSyntaxException {
		UUID id = player.getUUID();

		if (isOwner(id) && getMembers().size() > 1) {
			throw TeamArgument.OWNER_CANT_LEAVE.create();
		}

		PlayerTeam team = manager.getInternalPlayerTeam(id);
		team.actualTeam = team;

		team.ranks.put(id, TeamRank.OWNER);
		sendMessage(Util.NIL_UUID, new TranslatableComponent("ftbteams.message.left_party", player.getName().copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
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
		manager.syncAll();
		team.changedTeam(this, id, player, deleted);
		return Command.SINGLE_SUCCESS;
	}

	public int addAlly(CommandSourceStack source, Collection<GameProfile> players) throws CommandSyntaxException {
		UUID from = source.getEntity() == null ? Util.NIL_UUID : source.getEntity().getUUID();

		List<GameProfile> addedPlayers = new ArrayList<>();
		for (GameProfile player : players) {
			UUID id = player.getId();

			if (!isAlly(id)) {
				ranks.put(id, TeamRank.ALLY);
				sendMessage(from, new TranslatableComponent("ftbteams.message.add_ally",
						manager.getName(id).copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
				addedPlayers.add(player);
				ServerPlayer invitedPlayer = manager.getServer().getPlayerList().getPlayer(id);
				if (invitedPlayer != null) {
					invitedPlayer.displayClientMessage(new TranslatableComponent("ftbteams.message.now_allied", getDisplayName()).withStyle(ChatFormatting.GREEN), false);
				}
			}
		}

		if (!addedPlayers.isEmpty()) {
			save();
			manager.syncAll();
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
				sendMessage(from, new TranslatableComponent("ftbteams.message.remove_ally",
						manager.getName(id).copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
				removedPlayers.add(player);
				ServerPlayer removedPlayer = manager.getServer().getPlayerList().getPlayer(id);
				if (removedPlayer != null) {
					removedPlayer.displayClientMessage(new TranslatableComponent("ftbteams.message.no_longer_allied", getDisplayName()).withStyle(ChatFormatting.GOLD), false);
				}
			}
		}

		if (!removedPlayers.isEmpty()) {
			save();
			manager.syncAll();
			TeamEvent.REMOVE_ALLY.invoker().accept(new TeamAllyEvent(this, removedPlayers, false));
			return 1;
		}

		return 0;
	}

	public int listAllies(CommandSourceStack source) throws CommandSyntaxException {
		source.sendSuccess(new TextComponent("Allies:"), false);
		boolean any = false;

		for (Map.Entry<UUID, TeamRank> entry : getRanked(TeamRank.ALLY).entrySet()) {
			if (!entry.getValue().is(TeamRank.MEMBER)) {
				source.sendSuccess(manager.getName(entry.getKey()), false);
				any = true;
			}
		}

		if (!any) {
			source.sendSuccess(new TextComponent("None"), false);
		}

		return 1;
	}
}
