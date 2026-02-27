package dev.ftb.mods.ftbteams.client.gui;

import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.NordButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.Panel;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CreatePartyButton extends NordButton {
	private final boolean enabled;

	CreatePartyButton(Panel panel, boolean enabled) {
		super(panel, makeTitle(enabled), Icons.ADD);
		this.enabled = enabled;
	}

	private static Component makeTitle(boolean enabled) {
		MutableComponent c = Component.translatable("ftbteams.create_party");
		return enabled ? c : c.withStyle(ChatFormatting.DARK_GRAY);
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		list.translate(enabled ? "ftbteams.create_party.info" : "ftbteams.server_permissions_prevent");
		list.maxWidth = 130;
	}

	@Override
	public void onClicked(MouseButton button) {
		if (enabled) {
			new CreatePartyScreen().openGui();
		}
	}
}
