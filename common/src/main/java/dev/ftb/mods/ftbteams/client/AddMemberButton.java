package dev.ftb.mods.ftbteams.client;

import dev.ftb.mods.ftbguilibrary.icon.Icon;
import dev.ftb.mods.ftbguilibrary.utils.MouseButton;
import dev.ftb.mods.ftbguilibrary.widget.Panel;
import dev.ftb.mods.ftbteams.FTBTeams;
import net.minecraft.network.chat.TextComponent;

public class AddMemberButton extends NordButton {
	public AddMemberButton(Panel panel) {
		super(panel, new TextComponent("Add Member"), Icon.getIcon(FTBTeams.MOD_ID + ":textures/add.png"));
		setHeight(14);
	}

	@Override
	public void onClicked(MouseButton mouseButton) {
	}
}
