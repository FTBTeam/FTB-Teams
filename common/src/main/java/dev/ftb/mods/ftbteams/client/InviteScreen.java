package dev.ftb.mods.ftbteams.client;

import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.KnownClientPlayer;
import dev.ftb.mods.ftbteams.net.PlayerGUIOperationMessage;
import net.minecraft.network.chat.Component;

public class InviteScreen extends BaseInvitationScreen {
    public InviteScreen() {
        super(Component.translatable("ftbteams.gui.invite"));
    }

    @Override
    protected boolean shouldIncludePlayer(KnownClientPlayer player) {
        // any player who is online and not in a team
        return player.online && player.isValid() && player.isInternalTeam() && player != ClientTeamManager.INSTANCE.selfKnownPlayer;
    }

    @Override
    protected ExecuteButton makeExecuteButton() {
        return new ExecuteButton(Component.translatable("ftbteams.gui.send_invite"), Icons.ADD, () -> {
            new PlayerGUIOperationMessage(PlayerGUIOperationMessage.Operation.INVITE, invites).sendToServer();
            closeGui();
        }) {
            @Override
            public boolean isEnabled() {
                return !invites.isEmpty();
            }
        };
    }
}
