package dev.ftb.mods.ftbteams.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbteams.data.KnownClientPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class InvitedButton extends NordButton {
	public static Component checkbox(boolean b) {
		if (b) {
			return new TextComponent("☑").withStyle(ChatFormatting.GREEN);
		} else {
			return new TextComponent("☐");
		}
	}

	public final CreatePartyScreen screen;
	public final KnownClientPlayer player;

	public InvitedButton(Panel panel, CreatePartyScreen s, KnownClientPlayer p) {
		super(panel, new TextComponent("").append(checkbox(s.invitedMembers.contains(p.uuid))).append(" " + p.name), FaceIcon.getFace(p.getProfile()));
		screen = s;
		player = p;

		if (!player.teamId.equals(player.uuid)) {
			title = title.copy().withStyle(ChatFormatting.DARK_GRAY);
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
		if (player.teamId.equals(player.uuid)) {
			return;
		}

		if (screen.invitedMembers.contains(player.uuid)) {
			screen.invitedMembers.remove(player.uuid);
			title = new TextComponent("").append(checkbox(false)).append(" " + player.name);
		} else {
			screen.invitedMembers.add(player.uuid);
			title = new TextComponent("").append(checkbox(true)).append(" " + player.name);
		}
	}
}
