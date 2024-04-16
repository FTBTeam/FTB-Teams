package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.property.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.regex.Pattern;

public abstract class TeamBase {
	public static final StringProperty DISPLAY_NAME = new StringProperty(new ResourceLocation(FTBTeams.MOD_ID, "display_name"), "", Pattern.compile(".{3,}"));
	public static final StringProperty DESCRIPTION = new StringProperty(new ResourceLocation(FTBTeams.MOD_ID, "description"), "");
	public static final ColorProperty COLOR = new ColorProperty(new ResourceLocation(FTBTeams.MOD_ID, "color"), Color4I.WHITE);
	public static final BooleanProperty FREE_TO_JOIN = new BooleanProperty(new ResourceLocation(FTBTeams.MOD_ID, "free_to_join"), false);
	public static final IntProperty MAX_MSG_HISTORY_SIZE = new IntProperty(new ResourceLocation(FTBTeams.MOD_ID, "max_msg_history_size"), 1000);

	UUID id;
	public final TeamProperties properties;
	final Map<UUID, TeamRank> ranks;
	CompoundTag extraData;
	protected final List<TeamMessage> messageHistory;

	public TeamBase() {
		id = Util.NIL_UUID;
		ranks = new HashMap<>();
		properties = new TeamProperties();
		extraData = new CompoundTag();
		messageHistory = new LinkedList<>();
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

	public int getMaxMessageHistorySize() {
		return getProperty(MAX_MSG_HISTORY_SIZE);
	}

	public String getStringID() {
		String s = getDisplayName().replaceAll("\\W", "");
		return (s.length() > 50 ? s.substring(0, 50) : s) + "#" + getId().toString().substring(0, 8);
	}

	public Component getName() {
		TextComponent text = new TextComponent(getDisplayName());

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
		TextComponent text = new TextComponent(getDisplayName());
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

	public void addMessage(TeamMessage message) {
		addMessages(Collections.singleton(message));
	}

	public void addMessages(Collection<TeamMessage> messages) {
		messageHistory.addAll(messages);
		while (messageHistory.size() > getMaxMessageHistorySize()) {
			messageHistory.remove(0);
		}
		save();
	}

	public List<TeamMessage> getMessageHistory() {
		return messageHistory;
	}
}
