package dev.ftb.mods.ftbteams.api.event;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

public class PlayerTransferredTeamOwnershipEvent extends TeamEvent {
	@Nullable
	private final ServerPlayer from;
	private final Either<ServerPlayer,NameAndId> to;

	public PlayerTransferredTeamOwnershipEvent(Team team, @Nullable ServerPlayer from, ServerPlayer newOwner) {
		super(team);

		Validate.isTrue(team.isPartyTeam(), "team must be a party team!");
		this.from = from;
		this.to = Either.left(newOwner);
	}

	public PlayerTransferredTeamOwnershipEvent(Team team, @Nullable ServerPlayer from, NameAndId toProfile) {
		super(team);

		Validate.isTrue(team.isPartyTeam(), "team must be a party team!");
		this.from = from;
		this.to = Either.right(toProfile);
	}

	@Nullable
	public ServerPlayer getFrom() {
		return from;
	}

	@Nullable
	public ServerPlayer getTo() {
		return to.left().orElse(null);
	}

	public GameProfile getToProfile() {
		return to.map(Player::getGameProfile, nameAndId -> new GameProfile(nameAndId.id(), nameAndId.name()));
	}
}
