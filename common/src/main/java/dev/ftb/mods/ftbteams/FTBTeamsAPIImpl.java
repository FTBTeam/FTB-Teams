package dev.ftb.mods.ftbteams;

import dev.ftb.mods.ftbteams.api.CustomPartyCreationHandler;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.PartyCreationValidator;
import dev.ftb.mods.ftbteams.api.TeamMessage;
import dev.ftb.mods.ftbteams.api.client.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import dev.ftb.mods.ftbteams.data.TeamMessageImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public enum FTBTeamsAPIImpl implements FTBTeamsAPI.API {
    INSTANCE;

    private final List<PartyCreationValidator> creationValidators = new CopyOnWriteArrayList<>();

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
    public CustomPartyCreationHandler setCustomPartyCreationHandler(CustomPartyCreationHandler partyCreationOverride) {
        return null;
    }

    @Override
    public CustomPartyCreationHandler getCustomPartyCreationHandler() {
        return null;
    }

    @Override
    public void setPartyCreationFromAPIOnly(boolean apiOnly) {
        if (apiOnly) {
            addPartyCreationValidator(player -> PartyCreationValidator.CreationResult.fail(Component.empty()));
        } else {
            creationValidators.clear();
        }
    }

    @Override
    public void addPartyCreationValidator(PartyCreationValidator validator) {
        creationValidators.add(validator);
    }

    public PartyCreationValidator.CreationResult validatePartyCreation(ServerPlayer player) {
        for (var validator : creationValidators) {
            PartyCreationValidator.CreationResult result = validator.validatePartyCreation(player);
            if (result.status() != PartyCreationValidator.ResultStatus.PASS) {
                return result;
            }
        }

        return PartyCreationValidator.SUCCESS;
    }

    @Override
    public TeamMessage createMessage(UUID sender, Component text) {
        return new TeamMessageImpl(sender, System.currentTimeMillis(), text);
    }
}
