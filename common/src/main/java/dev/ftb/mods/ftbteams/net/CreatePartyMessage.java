package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.Team;
import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.networking.simple.BaseC2SMessage;
import me.shedaniel.architectury.networking.simple.MessageType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CreatePartyMessage extends BaseC2SMessage {
	private final String name;
	private final String description;
	private final int color;
	private final Set<UUID> invited;

	CreatePartyMessage(FriendlyByteBuf buffer) {
		name = buffer.readUtf(Short.MAX_VALUE);
		description = buffer.readUtf(Short.MAX_VALUE);
		color = buffer.readInt();
		int s = buffer.readVarInt();
		invited = new HashSet<>(s);

		for (int i = 0; i < s; i++) {
			invited.add(buffer.readUUID());
		}
	}

	public CreatePartyMessage(String n, String d, int c, Set<UUID> i) {
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

		for (UUID id : invited) {
			buffer.writeUUID(id);
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
