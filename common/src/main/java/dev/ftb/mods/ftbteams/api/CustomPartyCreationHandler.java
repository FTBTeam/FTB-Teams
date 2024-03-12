package dev.ftb.mods.ftbteams.api;

import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

/**
 * Deprecated: use {@link FTBTeamsAPI.API#setPartyCreationFromAPIOnly(boolean)}
 */
@FunctionalInterface
@Deprecated
public interface CustomPartyCreationHandler {
    void createParty(MouseButton button);
}
