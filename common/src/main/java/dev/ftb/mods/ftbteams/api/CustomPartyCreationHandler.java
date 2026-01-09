package dev.ftb.mods.ftbteams.api;


import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;

@FunctionalInterface
@Deprecated
public interface CustomPartyCreationHandler {
    void createParty(MouseButton button);
}
