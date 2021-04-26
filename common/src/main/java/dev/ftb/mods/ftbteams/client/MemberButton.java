package dev.ftb.mods.ftbteams.client;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbguilibrary.icon.FaceIcon;
import dev.ftb.mods.ftbguilibrary.utils.MouseButton;
import dev.ftb.mods.ftbguilibrary.widget.NordButton;
import dev.ftb.mods.ftbguilibrary.widget.Panel;
import dev.ftb.mods.ftbteams.data.TeamRank;
import net.minecraft.network.chat.TextComponent;

public class MemberButton extends NordButton {
	public final GameProfile profile;
	public final TeamRank rank;

	public MemberButton(Panel panel, GameProfile p, TeamRank r) {
		super(panel, new TextComponent(p.getName()), FaceIcon.getFace(p));
		profile = p;
		rank = r;
	}

	@Override
	public void onClicked(MouseButton mouseButton) {
	}
}
