package dev.ftb.mods.ftbteams.net;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.Team;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

public class CreatePartyMessage extends BaseC2SMessage {
	private final String name;
	private final String description;
	private final int color;
	private final Set<GameProfile> invited;

	CreatePartyMessage(FriendlyByteBuf buffer) {
		name = buffer.readUtf(Short.MAX_VALUE);
		description = buffer.readUtf(Short.MAX_VALUE);
		color = buffer.readInt();
		int s = buffer.readVarInt();
		invited = new HashSet<>(s);

		for (int i = 0; i < s; i++) {
			invited.add(new GameProfile(buffer.readUUID(), buffer.readUtf(Short.MAX_VALUE)));
		}
	}

	public CreatePartyMessage(String n, String d, int c, Set<GameProfile> i) {
		name = n;
		description = d;
		color = c;
		invited = i;
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.CREATE_PARTY;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(name, Short.MAX_VALUE);
		buffer.writeUtf(description, Short.MAX_VALUE);
		buffer.writeInt(color);
		buffer.writeVarInt(invited.size());

		for (GameProfile p : invited) {
			buffer.writeUUID(p.getId());
			buffer.writeUtf(p.getName(), Short.MAX_VALUE);
		}
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		Team team = FTBTeamsAPI.getPlayerTeam(player);

		if (team instanceof PlayerTeam) {
			((PlayerTeam) team).createParty(player, name, description, color, invited);
		}
	}
}
