package dev.ftb.mods.ftbteams.api;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface PartyCreationValidator {
    CreationResult SUCCESS = new CreationResult(ResultStatus.SUCCESS, Component.empty());
    CreationResult PASS = new CreationResult(ResultStatus.PASS, Component.empty());

    /**
     * Is the given player allowed to create a party team via GUI or command at this time?
     *
     * @param player the player to check
     * @return the creation outcome; see {@link FTBTeamsAPI.API#addPartyCreationValidator(PartyCreationValidator)}
     * for more details
     */
    CreationResult validatePartyCreation(ServerPlayer player);

    record CreationResult(ResultStatus status, Component reason) {
        public static CreationResult fail(Component reason) {
            return new CreationResult(ResultStatus.FAILURE, reason);
        }

        public boolean isSuccess() {
            return status == ResultStatus.SUCCESS;
        }
    }

    enum ResultStatus {
        SUCCESS,
        PASS,
        FAILURE
    }
}
