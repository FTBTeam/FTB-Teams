package dev.ftb.mods.ftbteams.event;

import dev.ftb.mods.ftbteams.property.TeamProperty;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class TeamCollectPropertiesEvent {
	private final Consumer<TeamProperty> callback;

	public TeamCollectPropertiesEvent(Consumer<TeamProperty> c) {
		callback = c;
	}

	public void add(TeamProperty property) {
		callback.accept(property);
	}
}