package dev.ftb.mods.ftbteams.data;

import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamMessage;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.api.property.TeamProperty;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.*;

/**
 * Base class for all teams, client and server side
 */
public abstract class AbstractTeamBase implements Team {
	protected final UUID id;
	protected final TeamPropertyCollectionImpl properties;
	protected final Map<UUID, TeamRank> ranks;
	protected CompoundTag extraData;
	protected final List<TeamMessage> messageHistory;
	private boolean valid;

	public AbstractTeamBase(UUID id) {
		this.id = id;
		ranks = new HashMap<>();
		properties = new TeamPropertyCollectionImpl();
		extraData = new CompoundTag();
		messageHistory = new LinkedList<>();
		valid = true;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public UUID getTeamId() {
		return id;
	}

	@Override
	public TeamPropertyCollection getProperties() {
		return properties;
	}

	public abstract TeamType getType();

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (o instanceof AbstractTeam) {
			return id.equals(((AbstractTeam) o).getId());
		}

		return false;
	}

	@Override
	public String toString() {
		return getShortName();
	}

	@Override
	public CompoundTag getExtraData() {
		return extraData;
	}

	@Override
	public <T> T getProperty(TeamProperty<T> property) {
		return properties.get(property);
	}

	@Override
	public <T> void setProperty(TeamProperty<T> property, T value) {
		properties.set(property, value);
		markDirty();
	}

	public String getDisplayName() {
		return getProperty(TeamProperties.DISPLAY_NAME);
	}

	public String getDescription() {
		return getProperty(TeamProperties.DESCRIPTION);
	}

	public int getColor() {
		return getProperty(TeamProperties.COLOR).rgb();
	}

	public boolean isFreeToJoin() {
		return getProperty(TeamProperties.FREE_TO_JOIN);
	}

	public int getMaxMessageHistorySize() {
		return getProperty(TeamProperties.MAX_MSG_HISTORY_SIZE);
	}

	@Override
	public String getShortName() {
		String s = getDisplayName().replaceAll("\\W", "_");
		return (s.length() > 50 ? s.substring(0, 50) : s) + "#" + getId().toString().substring(0, 8);
	}

	@Override
	public Component getName() {
		MutableComponent text = Component.literal(getDisplayName());

		if (getType().isPlayer()) {
			text.withStyle(ChatFormatting.GRAY);
		} else if (getType().isServer()) {
			text.withStyle(ChatFormatting.RED);
		} else {
			text.withStyle(ChatFormatting.AQUA);
		}

		text.setStyle(text.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams info " + getShortName())));
		return text;
	}

	@Override
	public Component getColoredName() {
		MutableComponent text = Component.literal(getDisplayName());
		text.withStyle(getProperty(TeamProperties.COLOR).toStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams info " + getShortName())));
		return text;
	}

	@Override
	public void markDirty() {
	}

	@Override
	public TeamRank getRankForPlayer(UUID playerId) {
		TeamRank rank = ranks.get(playerId);
		if (rank != null) {
			return rank;
		}

		return isFreeToJoin() ? TeamRank.INVITED : TeamRank.NONE;

	}

	public boolean isMember(UUID uuid) {
		return getRankForPlayer(uuid).isMemberOrBetter();
	}

	@Override
	public Map<UUID, TeamRank> getPlayersByRank(TeamRank minRank) {
		if (minRank == TeamRank.NONE) {
			return Collections.unmodifiableMap(ranks);
		}

		Map<UUID, TeamRank> map = new HashMap<>();
		ranks.forEach((id, rank) -> {
			if (rank.isAtLeast(minRank)) {
				map.put(id, rank);
			}
		});

		return Collections.unmodifiableMap(map);
	}

	@Override
	public String getTypeTranslationKey() {
		return "ftbteams.team_type." + getType().getSerializedName();
	}

	@Override
	public Set<UUID> getMembers() {
		return getPlayersByRank(TeamRank.MEMBER).keySet();
	}

	public boolean isAllyOrBetter(UUID profile) {
		return getRankForPlayer(profile).isAllyOrBetter();
	}

	public boolean isOfficerOrBetter(UUID profile) {
		return getRankForPlayer(profile).isOfficerOrBetter();
	}

	public boolean isInvited(UUID profile) {
		return getRankForPlayer(profile).isInvitedOrBetter();
	}

	public void addMessage(TeamMessage message) {
		addMessages(List.of(message));
	}

	public void addMessages(Collection<TeamMessage> messages) {
		messageHistory.addAll(messages);
		while (messageHistory.size() > getMaxMessageHistorySize()) {
			messageHistory.remove(0);
		}
		markDirty();
	}

	@Override
	public List<TeamMessage> getMessageHistory() {
		return Collections.unmodifiableList(messageHistory);
	}

	public void addMember(UUID id, TeamRank rank) {
		ranks.put(id, rank);
	}

	public void removeMember(UUID id) {
		ranks.remove(id);
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	public void invalidateTeam() {
		valid = false;
	}
}
