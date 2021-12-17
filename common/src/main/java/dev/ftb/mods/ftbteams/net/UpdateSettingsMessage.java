package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import dev.ftb.mods.ftbteams.event.TeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class UpdateSettingsMessage extends BaseC2SMessage {
	public final TeamProperties properties;

	UpdateSettingsMessage(FriendlyByteBuf buffer) {
		properties = new TeamProperties();
		properties.read(buffer);
	}

	public UpdateSettingsMessage(TeamProperties p) {
		properties = p;
	}

	@Override
	public MessageType getType() {
		return FTBTeamsNet.UPDATE_SETTINGS;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		properties.write(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		Team team = FTBTeamsAPI.getPlayerTeam(player);

		if (!team.isOfficer(player.getUUID())) {
			return;
		}

		TeamProperties old = team.properties.copy();
		team.properties.updateFrom(properties);
		TeamEvent.PROPERTIES_CHANGED.invoker().accept(new TeamPropertiesChangedEvent(team, old));
		team.save();
		new UpdateSettingsResponseMessage(team.getId(), team.properties).sendToAll(player.server);
	}
}
