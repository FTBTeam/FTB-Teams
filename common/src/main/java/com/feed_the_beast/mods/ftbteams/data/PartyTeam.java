package com.feed_the_beast.mods.ftbteams.data;

import com.mojang.authlib.GameProfile;
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

public class PartyTeam extends Team {
	final HashSet<GameProfile> invited;
	final HashSet<GameProfile> officers;
	final List<TeamMessage> messageHistory;
	final HashSet<GameProfile> allies;

	public PartyTeam(TeamManager m) {
		super(m);
		invited = new HashSet<>();
		officers = new HashSet<>();
		messageHistory = new ArrayList<>();
		allies = new HashSet<>();
	}

	@Override
	public TeamType getType() {
		return TeamType.PARTY;
	}

	public Set<GameProfile> getAllies() {
		return allies;
	}

	public Set<GameProfile> getOfficers() {
		return officers;
	}

	public Set<GameProfile> getInvited() {
		return invited;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = super.serializeNBT();

		ListTag invitedNBT = new ListTag();

		for (GameProfile invited : invited) {
			invitedNBT.add(StringTag.valueOf(FTBTUtils.serializeProfile(invited)));
		}

		tag.put("invited", invitedNBT);

		ListTag alliesNBT = new ListTag();

		for (GameProfile ally : allies) {
			alliesNBT.add(StringTag.valueOf(FTBTUtils.serializeProfile(ally)));
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
			GameProfile profile = FTBTUtils.deserializeProfile(invitedNBT.getString(i));

			if (profile != FTBTUtils.NO_PROFILE) {
				invited.add(profile);
			}
		}

		allies.clear();
		ListTag alliesNBT = tag.getList("allies", NbtType.STRING);

		for (int i = 0; i < alliesNBT.size(); i++) {
			GameProfile profile = FTBTUtils.deserializeProfile(alliesNBT.getString(i));

			if (profile != FTBTUtils.NO_PROFILE) {
				allies.add(profile);
			}
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
