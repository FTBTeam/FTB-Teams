package com.feed_the_beast.mods.ftbteams.data;

import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;

public class OrderedCompoundTag extends CompoundTag {
	public OrderedCompoundTag() {
		super(new LinkedHashMap<>());
	}
}
