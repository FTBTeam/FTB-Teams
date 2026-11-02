package dev.ftb.mods.ftbteams.client.gui;

import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.NordButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.Panel;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.data.PlayerPermissions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CreatePartyButton extends NordButton {
	private final PlayerPermissions permissions;

	CreatePartyButton(Panel panel, PlayerPermissions permissions) {
		super(panel, makeTitle(permissions.createParty()), Icons.ADD);
		this.permissions = permissions;
	}

	private static Component makeTitle(boolean enabled) {
		MutableComponent c = Component.translatable("ftbteams.create_party");
		return enabled ? c : c.withStyle(ChatFormatting.DARK_GRAY);
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		if (permissions.createParty()) {
			list.translate("ftbteams.create_party.info");
		} else {
			list.add(permissions.partyPreventionReason());
		}
		list.maxWidth = 130;
	}

	@Override
	public void onClicked(MouseButton button) {
		if (permissions.createParty()) {
			new CreatePartyScreen().openGui();
		}
	}
}
