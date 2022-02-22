package dev.ftb.mods.ftbteams.client;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.network.chat.TranslatableComponent;

public class CreatePartyButton extends NordButton {
	public CreatePartyButton(Panel panel) {
		super(panel, new TranslatableComponent("ftbteams.create_party"), Icon.getIcon(FTBTeams.MOD_ID + ":textures/add.png"));
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
