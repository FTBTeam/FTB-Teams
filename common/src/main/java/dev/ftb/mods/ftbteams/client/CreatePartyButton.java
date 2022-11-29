package dev.ftb.mods.ftbteams.client;

import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.network.chat.Component;

public class CreatePartyButton extends NordButton {
	CreatePartyButton(Panel panel) {
		super(panel, Component.translatable("ftbteams.create_party"), Icons.ADD);
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		list.translate("ftbteams.create_party.info");
		list.maxWidth = 130;
	}

	@Override
	public void onClicked(MouseButton button) {
		if (FTBTeamsAPI.partyCreationOverride != null) {
			FTBTeamsAPI.partyCreationOverride.accept(button);
		} else {
			new CreatePartyScreen().openGui();
		}
	}
}
