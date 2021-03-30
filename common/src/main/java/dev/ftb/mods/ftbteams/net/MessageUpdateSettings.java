package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.event.TeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class MessageUpdateSettings extends MessageBase {
	public final TeamProperties properties;

	MessageUpdateSettings(FriendlyByteBuf buffer) {
		properties = new TeamProperties();
		properties.read(buffer);
	}

	public MessageUpdateSettings(TeamProperties p) {
		properties = p;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		properties.write(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		Team team = FTBTeamsAPI.getPlayerTeam(player.getUUID());

		if (team == null || !team.isOfficer(player.getUUID())) {
			return;
		}

		TeamProperties old = team.properties.copy();
		team.properties.updateFrom(properties);
		TeamPropertiesChangedEvent.EVENT.invoker().accept(new TeamPropertiesChangedEvent(team, old));
		team.save();
	}
}
