package dev.ftb.mods.ftbteams.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbteams.data.KnownClientPlayer;
import dev.ftb.mods.ftbteams.data.TeamRank;
import net.minecraft.network.chat.TextComponent;

public class MemberButton extends NordButton {
	public final KnownClientPlayer player;
	public final TeamRank rank;

	public MemberButton(Panel panel, KnownClientPlayer p, TeamRank r) {
		super(panel, new TextComponent(p.name), FaceIcon.getFace(p.getProfile()));
		player = p;
		rank = r;
	}

	@Override
	public void drawIcon(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.drawIcon(matrixStack, theme, x, y, w, h);

		if (player.online) {
			matrixStack.pushPose();
			matrixStack.translate(x + w - 1.5D, y - 0.5D, 0);
			Color4I.GREEN.draw(matrixStack, 0, 0, 2, 2);
			matrixStack.popPose();
		}
	}

	@Override
	public void onClicked(MouseButton button) {
	}
}
