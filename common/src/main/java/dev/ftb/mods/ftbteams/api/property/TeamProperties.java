package dev.ftb.mods.ftbteams.api.property;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * These are the standard team properties which are registered for every team. Other mods may add additional properties;
 * see {@link dev.ftb.mods.ftbteams.api.event.TeamCollectPropertiesEvent}.
 */
public class TeamProperties {
    public static final StringProperty DISPLAY_NAME
            = (StringProperty) new StringProperty(FTBTeamsAPI.id("display_name"), "", Pattern.compile(".{3,}"))
            .syncToAll();
    public static final StringProperty DESCRIPTION
            = new StringProperty(FTBTeamsAPI.id("description"), "");
    public static final ColorProperty COLOR
            = (ColorProperty) new ColorProperty(FTBTeamsAPI.id("color"), Color4I.WHITE)
            .syncToAll();
    public static final BooleanProperty FREE_TO_JOIN
            = new BooleanProperty(FTBTeamsAPI.id("free_to_join"), false);
    public static final IntProperty MAX_MSG_HISTORY_SIZE
            = new IntProperty(FTBTeamsAPI.id("max_msg_history_size"), 1000);
    public static final StringSetProperty TEAM_STAGES
            = (StringSetProperty) new StringSetProperty(FTBTeamsAPI.id("team_stages"), new HashSet<>())
            .hidden()
            .notPlayerEditable();
}
