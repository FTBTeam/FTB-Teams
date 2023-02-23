package dev.ftb.mods.ftbteams.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.client.MyTeamScreen;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamMessage;
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
            messages.add(TeamMessage.fromNetwork(now, buf));
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
            tm.toNetwork(now, buf);
        }
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        ClientTeam team = ClientTeamManager.INSTANCE.selfTeam();
        if (team != null) {
            team.setMessageHistory(messages);
            MyTeamScreen.refreshIfOpen();
        }
    }
}
