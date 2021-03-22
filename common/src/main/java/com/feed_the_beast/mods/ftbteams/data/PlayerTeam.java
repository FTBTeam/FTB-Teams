package com.feed_the_beast.mods.ftbteams.data;

import com.mojang.util.UUIDTypeAdapter;
import me.shedaniel.architectury.utils.NbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerTeam extends Team {
	final Set<UUID> allies;

	public PlayerTeam(TeamManager m) {
		super(m);
		allies = new HashSet<>();
	}

	@Override
	public TeamType getType() {
		return TeamType.PLAYER;
	}

	public Set<UUID> getAllies() {
		return allies;
	}

	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = super.serializeNBT();

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

		allies.clear();
		ListTag alliesNBT = tag.getList("allies", NbtType.STRING);

		for (int i = 0; i < alliesNBT.size(); i++) {
			allies.add(UUIDTypeAdapter.fromString(alliesNBT.getString(i)));
		}
	}
}
