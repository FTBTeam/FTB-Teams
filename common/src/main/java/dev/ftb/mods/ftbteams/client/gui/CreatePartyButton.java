package dev.ftb.mods.ftbteams.client.gui;

import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.FTBTeamsAPIImpl;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
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
		if (FTBTeamsAPI.api().getCustomPartyCreationHandler() != null) {
			FTBTeamsAPIImpl.INSTANCE.getCustomPartyCreationHandler().createParty(button);
		} else {
			new CreatePartyScreen().openGui();
		}
	}
}
