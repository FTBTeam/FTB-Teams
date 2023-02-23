package dev.ftb.mods.ftbteams.client;

import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.KnownClientPlayer;
import dev.ftb.mods.ftbteams.net.PlayerGUIOperationMessage;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;

public class AllyScreen extends BaseInvitationScreen {
    private final Set<GameProfile> existingAllies = new HashSet<>();
    private Set<GameProfile> toAdd = new HashSet<>();
    private Set<GameProfile> toRemove = new HashSet<>();

    public AllyScreen() {
        super(Component.translatable("ftbteams.gui.manage_allies"));

        available.forEach(knownClientPlayer -> {
            if (ClientTeamManager.INSTANCE.selfTeam().isAlly(knownClientPlayer.id())) {
                existingAllies.add(knownClientPlayer.profile());
                invites.add(knownClientPlayer.profile());
            }
        });
    }

    @Override
    protected boolean shouldIncludePlayer(KnownClientPlayer player) {
        // any player who isn't in our team is a valid potential or actual ally
        return player.online() && !ClientTeamManager.INSTANCE.selfTeam().isMember(player.id());
    }

    @Override
    public void setInvited(GameProfile profile, boolean invited) {
        super.setInvited(profile, invited);

        toRemove = Sets.difference(existingAllies, invites);
        toAdd = Sets.difference(invites, existingAllies);
    }

    @Override
    protected ExecuteButton makeExecuteButton() {
        return new ExecuteButton(Component.translatable("gui.accept"), Icons.ADD, () -> {
            if (!toAdd.isEmpty()) {
                new PlayerGUIOperationMessage(PlayerGUIOperationMessage.Operation.ADD_ALLY, toAdd).sendToServer();
            }
            if (!toRemove.isEmpty()) {
                new PlayerGUIOperationMessage(PlayerGUIOperationMessage.Operation.REMOVE_ALLY, toRemove).sendToServer();
            }
            closeGui();
        }) {
            @Override
            public boolean isEnabled() {
                return !toAdd.isEmpty() || !toRemove.isEmpty();
            }
        };
    }
}
