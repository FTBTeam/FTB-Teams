package dev.ftb.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.event.PlayerTransferredTeamOwnershipEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
		tag.putString("owner", owner.toString());
		return tag;
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

		UUID id = player.getUUID();

		manager.playerTeamMap.put(id, this);
		ranks.put(id, TeamRank.MEMBER);
		changedTeam(oldTeam, id, player);
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
			if (isMember(player.getId())) {
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
				accept.setStyle(accept.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ftbteams party join " + getStringID())));
				accept.withStyle(ChatFormatting.GREEN);
				playerEntity.displayClientMessage(accept, false);

				TextComponent deny = new TextComponent("[Click here to deny the invite]");
				deny.withStyle(ChatFormatting.RED);
				deny.setStyle(deny.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ftbteams party deny_invite " + getStringID())));
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
				throw TeamArgument.CANT_KICK_OWNER.create();
			}

			PlayerTeam team = manager.getInternalPlayerTeam(id);
			manager.playerTeamMap.put(id, team);

			ServerPlayer playerEntity = FTBTUtils.getPlayerByUUID(manager.getServer(), id);

			team.ranks.put(id, TeamRank.OWNER);
			team.changedTeam(this, id, playerEntity);
			sendMessage(from.getUUID(), new TextComponent("Kicked ").append(manager.getName(id)).append(" from ").append(getName()).withStyle(ChatFormatting.RED));
			team.save();

			ranks.remove(id);
			save();
			manager.syncAll();

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

		if (from == to) {
			from.sendMessage(new TextComponent("What."), Util.NIL_UUID);
			return 0;
		}

		ranks.put(owner, TeamRank.OFFICER);
		owner = to.getUUID();
		ranks.put(owner, TeamRank.OWNER);
		save();
		TeamEvent.OWNERSHIP_TRANSFERRED.invoker().accept(new PlayerTransferredTeamOwnershipEvent(this, from, to));

		sendMessage(from.getUUID(), new TextComponent("Transferred ownership to ").append(to.getDisplayName()).withStyle(ChatFormatting.RED));
		updateCommands(from);
		updateCommands(to);
		manager.syncAll();
		return Command.SINGLE_SUCCESS;
	}

	@Deprecated
	public int leave(ServerPlayer player) throws CommandSyntaxException {
		UUID id = player.getUUID();

		if (isOwner(id) && getMembers().size() > 1) {
			throw TeamArgument.OWNER_CANT_LEAVE.create();
		}

		PlayerTeam team = manager.getInternalPlayerTeam(id);
		manager.playerTeamMap.put(id, team);

		team.ranks.put(id, TeamRank.OWNER);
		team.changedTeam(this, id, player);
		sendMessage(Util.NIL_UUID, new TextComponent("").append(player.getName()).append(" left your party!").withStyle(ChatFormatting.YELLOW));
		team.save();

		ranks.remove(id);
		manager.save();

		if (getMembers().isEmpty()) {
			TeamEvent.DELETED.invoker().accept(new TeamEvent(this));
			manager.saveNow();
			manager.teamMap.remove(getId());
			String fn = getId() + ".nbt";

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

		manager.syncAll();
		return Command.SINGLE_SUCCESS;
	}
}
