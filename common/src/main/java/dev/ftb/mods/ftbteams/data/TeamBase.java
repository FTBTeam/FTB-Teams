package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.property.BooleanProperty;
import dev.ftb.mods.ftbteams.property.ColorProperty;
import dev.ftb.mods.ftbteams.property.StringProperty;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import dev.ftb.mods.ftbteams.property.TeamProperty;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public abstract class TeamBase {
	public static final StringProperty DISPLAY_NAME = new StringProperty(new ResourceLocation(FTBTeams.MOD_ID, "display_name"), "", Pattern.compile(".{3,}"));
	public static final StringProperty DESCRIPTION = new StringProperty(new ResourceLocation(FTBTeams.MOD_ID, "description"), "");
	public static final ColorProperty COLOR = new ColorProperty(new ResourceLocation(FTBTeams.MOD_ID, "color"), Color4I.WHITE);
	public static final BooleanProperty FREE_TO_JOIN = new BooleanProperty(new ResourceLocation(FTBTeams.MOD_ID, "free_to_join"), false);

	UUID id;
	public final TeamProperties properties;
	final Map<UUID, TeamRank> ranks;
	CompoundTag extraData;
	public final List<TeamMessage> messageHistory;

	public TeamBase() {
		id = Util.NIL_UUID;
		ranks = new HashMap<>();
		properties = new TeamProperties();
		extraData = new CompoundTag();
		messageHistory = new ArrayList<>();
	}

	public abstract TeamType getType();

	public abstract boolean isValid();

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (o instanceof Team) {
			return id.equals(((Team) o).getId());
		}

		return false;
	}

	@Override
	public String toString() {
		return getStringID();
	}

	public UUID getId() {
		return id;
	}

	public CompoundTag getExtraData() {
		return extraData;
	}

	public <T> T getProperty(TeamProperty<T> property) {
		return properties.get(property);
	}

	public <T> void setProperty(TeamProperty<T> property, T value) {
		properties.set(property, value);
		save();
	}

	public String getDisplayName() {
		return getProperty(DISPLAY_NAME);
	}

	public String getDescription() {
		return getProperty(DESCRIPTION);
	}

	public int getColor() {
		return getProperty(COLOR).rgb();
	}

	public boolean isFreeToJoin() {
		return getProperty(FREE_TO_JOIN);
	}

	public String getStringID() {
		String s = getDisplayName().replaceAll("\\W", "");
		return (s.length() > 50 ? s.substring(0, 50) : s) + "#" + getId().toString().substring(0, 8);
	}

	public Component getName() {
		MutableComponent text = Component.literal(getDisplayName());

		if (getType().isPlayer()) {
			text.withStyle(ChatFormatting.GRAY);
		} else if (getType().isServer()) {
			text.withStyle(ChatFormatting.RED);
		} else {
			text.withStyle(ChatFormatting.AQUA);
		}

		text.setStyle(text.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams info " + getStringID())));
		return text;
	}

	public Component getColoredName() {
		MutableComponent text = Component.literal(getDisplayName());
		text.withStyle(getProperty(COLOR).toStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams info " + getStringID())));
		return text;
	}

	public void save() {
	}

	public TeamRank getHighestRank(UUID playerId) {
		TeamRank rank = ranks.get(playerId);

		if (rank != null) {
			return rank;
		}

		if (getProperty(Team.FREE_TO_JOIN)) {
			return TeamRank.INVITED;
		}

		return TeamRank.NONE;
	}

	public boolean isMember(UUID uuid) {
		return getHighestRank(uuid).isMember();
	}

	public Map<UUID, TeamRank> getRanked(TeamRank rank) {
		if (rank == TeamRank.NONE) {
			return ranks;
		}

		Map<UUID, TeamRank> map = new HashMap<>();

		for (Map.Entry<UUID, TeamRank> entry : ranks.entrySet()) {
			if (entry.getValue().is(rank)) {
				map.put(entry.getKey(), entry.getValue());
			}
		}

		return map;
	}

	public Set<UUID> getMembers() {
		return getRanked(TeamRank.MEMBER).keySet();
	}

	public boolean isAlly(UUID profile) {
		return getHighestRank(profile).isAlly();
	}

	public boolean isOfficer(UUID profile) {
		return getHighestRank(profile).isOfficer();
	}

	public boolean isInvited(UUID profile) {
		return getHighestRank(profile).isInvited();
	}
}
