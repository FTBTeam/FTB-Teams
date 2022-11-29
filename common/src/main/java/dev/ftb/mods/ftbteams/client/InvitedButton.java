package dev.ftb.mods.ftbteams.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.NordColors;
import dev.ftb.mods.ftbteams.data.KnownClientPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;


public class InvitedButton extends NordButton {
	public final InvitationSetup screen;

	public final KnownClientPlayer player;

	InvitedButton(Panel panel, InvitationSetup s, KnownClientPlayer p) {
		super(panel, checkbox(s.isInvited(p.getProfile())).append(" " + p.name), FaceIcon.getFace(p.getProfile()));

		screen = s;
		player = p;

		if (!player.isValid()) {
			title = title.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(NordColors.POLAR_NIGHT_0.rgb())));
		}
	}

	private static MutableComponent checkbox(boolean checked) {
		return checked ? Component.literal("☑").withStyle(ChatFormatting.GREEN) : Component.literal("☐");
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
		if (player.isValid()) {
			GameProfile profile = player.getProfile();
			boolean invited = screen.isInvited(profile);
			screen.setInvited(profile, !invited);
			title = checkbox(!invited).append(" " + player.name);
		}
	}
}
