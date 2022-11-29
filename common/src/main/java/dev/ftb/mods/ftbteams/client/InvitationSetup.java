package dev.ftb.mods.ftbteams.client;

import com.mojang.authlib.GameProfile;

public interface InvitationSetup {
    boolean isInvited(GameProfile profile);
    void setInvited(GameProfile profile, boolean invited);
}
