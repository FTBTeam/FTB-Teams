package dev.ftb.mods.ftbteams.client;

import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbguilibrary.icon.Icon;
import dev.ftb.mods.ftbguilibrary.utils.MouseButton;
import dev.ftb.mods.ftbguilibrary.widget.Panel;
import dev.ftb.mods.ftbteams.data.TeamRank;
import net.minecraft.network.chat.TextComponent;

public class MemberButton extends NordButton {
	public final GameProfile profile;
	public final TeamRank rank;

	public MemberButton(Panel panel, GameProfile p, TeamRank r) {
		super(panel, new TextComponent(p.getName()), Icon.getIcon("https://minotar.net/avatar/" + UUIDTypeAdapter.fromUUID(p.getId()) + "/8"));
		setHeight(14);
		profile = p;
		rank = r;
	}

	@Override
	public void onClicked(MouseButton mouseButton) {
	}
}
