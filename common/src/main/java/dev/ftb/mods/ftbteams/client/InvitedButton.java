package dev.ftb.mods.ftbteams.client;

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
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;


public class InvitedButton extends NordButton {
	public static Component checkbox(boolean b) {
		if (b) {
			return Component.literal("☑").withStyle(ChatFormatting.GREEN);
		} else {
			return Component.literal("☐");
		}
	}

	public final CreatePartyScreen screen;
	public final KnownClientPlayer player;

	public InvitedButton(Panel panel, CreatePartyScreen s, KnownClientPlayer p) {
		super(panel, Component.literal("").append(checkbox(s.invitedMembers.contains(p.uuid))).append(" " + p.name), FaceIcon.getFace(p.getProfile()));
		screen = s;
		player = p;

		if (!player.isValid()) {
			title = title.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(NordColors.POLAR_NIGHT_0.rgb())));
		}
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
		if (!player.isValid()) {
			return;
		}

		if (screen.invitedMembers.contains(player.getProfile())) {
			screen.invitedMembers.remove(player.getProfile());
			title = Component.literal("").append(checkbox(false)).append(" " + player.name);
		} else {
			screen.invitedMembers.add(player.getProfile());
			title = Component.literal("").append(checkbox(true)).append(" " + player.name);
		}
	}
}
