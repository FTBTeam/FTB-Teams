package dev.ftb.mods.ftbteams;

import dev.ftb.mods.ftbteams.api.CustomPartyCreationHandler;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamMessage;
import dev.ftb.mods.ftbteams.api.client.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import dev.ftb.mods.ftbteams.data.TeamMessageImpl;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.UUID;

public enum FTBTeamsAPIImpl implements FTBTeamsAPI.API {
    INSTANCE;

    private CustomPartyCreationHandler partyCreationOverride = null;

    @Override
    public boolean isManagerLoaded() {
        return TeamManagerImpl.INSTANCE != null;
    }

    @Override
    public TeamManagerImpl getManager() {
        return Objects.requireNonNull(TeamManagerImpl.INSTANCE);
    }

    @Override
    public boolean isClientManagerLoaded() {
        return ClientTeamManagerImpl.getInstance() != null;
    }

    @Override
    public ClientTeamManager getClientManager() {
        return Objects.requireNonNull(ClientTeamManagerImpl.getInstance());
    }

    @Override
    public CustomPartyCreationHandler getCustomPartyCreationHandler() {
        return partyCreationOverride;
    }

    @Override
    public TeamMessage createMessage(UUID sender, Component text) {
        return new TeamMessageImpl(sender, System.currentTimeMillis(), text);
    }

    @Override
    public CustomPartyCreationHandler setCustomPartyCreationHandler(CustomPartyCreationHandler partyCreationOverride) {
        CustomPartyCreationHandler prev = this.partyCreationOverride;
        this.partyCreationOverride = partyCreationOverride;
        return prev;
    }


}
