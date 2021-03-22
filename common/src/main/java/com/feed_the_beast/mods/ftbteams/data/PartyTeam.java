package com.feed_the_beast.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import me.shedaniel.architectury.utils.NbtType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PartyTeam extends Team {
	final Set<UUID> invited;
	final Set<UUID> officers;
	final Set<UUID> allies;
	public final List<TeamMessage> messageHistory;

	public PartyTeam(TeamManager m) {
		super(m);
		invited = new HashSet<>();
		officers = new HashSet<>();
		allies = new HashSet<>();
		messageHistory = new ArrayList<>();
	}

	@Override
	public TeamType getType() {
		return TeamType.PARTY;
	}

	public Set<UUID> getAllies() {
		return allies;
	}

	public Set<UUID> getOfficers() {
		return officers;
	}

	public Set<UUID> getInvited() {
		return invited;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = super.serializeNBT();

		ListTag invitedNBT = new ListTag();

		for (UUID invited : invited) {
			invitedNBT.add(StringTag.valueOf(UUIDTypeAdapter.fromUUID(invited)));
		}

		tag.put("invited", invitedNBT);

		ListTag alliesNBT = new ListTag();

		for (UUID ally : allies) {
			alliesNBT.add(StringTag.valueOf(UUIDTypeAdapter.fromUUID(ally)));
		}

		tag.put("allies", alliesNBT);

		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		super.deserializeNBT(tag);

		invited.clear();
		ListTag invitedNBT = tag.getList("invited", NbtType.STRING);

		for (int i = 0; i < invitedNBT.size(); i++) {
			invited.add(UUIDTypeAdapter.fromString(invitedNBT.getString(i)));
		}

		allies.clear();
		ListTag alliesNBT = tag.getList("allies", NbtType.STRING);

		for (int i = 0; i < alliesNBT.size(); i++) {
			allies.add(UUIDTypeAdapter.fromString(alliesNBT.getString(i)));
		}
	}

	public void sendMessage(GameProfile from, Component text) {
		messageHistory.add(new TeamMessage(from, Instant.now(), text));

		if (messageHistory.size() > 100) {
			messageHistory.remove(0);
		}

		TextComponent component = new TextComponent("<");
		component.append(new TextComponent(from.getName()).withStyle(ChatFormatting.YELLOW));
		component.append(" @");
		component.append(getName());
		component.append("> ");
		component.append(text);

		for (ServerPlayer p : getOnlineMembers()) {
			p.displayClientMessage(component, false);
		}
	}
}
