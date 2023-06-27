package dev.ftb.mods.ftbteams.client;

import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CreatePartyButton extends NordButton {
	private final boolean enabled;

	CreatePartyButton(Panel panel, boolean enabled) {
		super(panel, makeTitle(enabled), enabled ? Icons.ADD : Icons.CANCEL);
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
			if (FTBTeamsAPI.partyCreationOverride != null) {
				FTBTeamsAPI.partyCreationOverride.accept(button);
			} else {
				new CreatePartyScreen().openGui();
			}
		}
	}
}
