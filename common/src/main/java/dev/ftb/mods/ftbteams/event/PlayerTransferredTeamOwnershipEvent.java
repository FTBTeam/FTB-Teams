package dev.ftb.mods.ftbteams.event;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;


/**
 * @author LatvianModder
 */
public class PlayerTransferredTeamOwnershipEvent extends TeamEvent {
	private final ServerPlayer from, to;
	private final GameProfile toProfile;

	public PlayerTransferredTeamOwnershipEvent(Team t, ServerPlayer pf, ServerPlayer pt) {
		super(t);
		from = pf;
		to = pt;
		toProfile = null;
	}

	public PlayerTransferredTeamOwnershipEvent(PartyTeam t, ServerPlayer from, GameProfile toProfile) {
		super(t);
		this.from = from;
		this.to = null;
		this.toProfile = toProfile;
	}

	@Nullable
	public ServerPlayer getFrom() {
		return from;
	}

	@Nullable
	public ServerPlayer getTo() {
		return to;
	}

	public GameProfile getToProfile() {
		return to == null ? toProfile : to.getGameProfile();
	}
}