package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftblibrary.net.BasePacket;
import dev.ftb.mods.ftblibrary.net.PacketID;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import dev.ftb.mods.ftbteams.event.TeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class UpdateSettingsPacket extends BasePacket {
	public final TeamProperties properties;

	UpdateSettingsPacket(FriendlyByteBuf buffer) {
		properties = new TeamProperties();
		properties.read(buffer);
	}

	public UpdateSettingsPacket(TeamProperties p) {
		properties = p;
	}

	@Override
	public PacketID getId() {
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
		new UpdateSettingsResponsePacket(team.getId(), team.properties).sendToAll();
	}
}
