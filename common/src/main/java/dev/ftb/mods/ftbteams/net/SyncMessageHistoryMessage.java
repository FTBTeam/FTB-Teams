package dev.ftb.mods.ftbteams.net;

import dev.ftb.mods.ftbteams.client.MyTeamScreen;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamMessage;
import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.networking.simple.BaseS2CMessage;
import me.shedaniel.architectury.networking.simple.MessageType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class SyncMessageHistoryMessage extends BaseS2CMessage {
    private final List<TeamMessage> messages;

    public SyncMessageHistoryMessage(FriendlyByteBuf buf) {
        long now = System.currentTimeMillis();
        int nMessages = buf.readVarInt();
        messages = new ArrayList<>(nMessages);
        for (int i = 0; i < nMessages; i++) {
            messages.add(new TeamMessage(now, buf));
        }
    }

    public SyncMessageHistoryMessage(Team team) {
        messages = team.getMessageHistory();
    }

    @Override
    public MessageType getType() {
        return FTBTeamsNet.SYNC_MESSAGE_HISTORY;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        long now = System.currentTimeMillis();
        buf.writeVarInt(messages.size());
        for (TeamMessage tm : messages) {
            tm.write(now, buf);
        }
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        ClientTeam team = ClientTeamManager.INSTANCE.selfTeam;
        if (team != null) {
            team.setMessageHistory(messages);
            MyTeamScreen.refreshIfOpen();
        }
    }
}
