package dev.ftb.mods.ftbteams.client.gui;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.theme.NordColors;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.NordButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.Panel;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;


public class InvitedButton extends NordButton {
	public final InvitationSetup screen;
	public final KnownClientPlayer player;

	InvitedButton(Panel panel, InvitationSetup setup, KnownClientPlayer knownClientPlayer) {
		super(panel, checkbox(setup.isInvited(knownClientPlayer.profile())).append(" " + knownClientPlayer.name()), FaceIcon.getFace(knownClientPlayer.profile(), true));

		screen = setup;
		player = knownClientPlayer;

		if (!player.online()) {
			title = title.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(NordColors.POLAR_NIGHT_0.rgb())));
		}
	}

	private static MutableComponent checkbox(boolean checked) {
		return checked ? Component.literal("☑").withStyle(ChatFormatting.GREEN) : Component.literal("☐");
	}

	@Override
	public void drawIcon(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
		super.drawIcon(graphics, theme, x, y, w, h);

		if (player.online()) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(x + w - 1.5F, y - 0.5F);
			IconHelper.renderIcon(Color4I.GREEN, graphics, 0, 0, 2, 2);
			graphics.pose().popMatrix();
		}
	}

	@Override
	public void onClicked(MouseButton button) {
		if (player.online()) {
			GameProfile profile = player.profile();
			boolean invited = screen.isInvited(profile);
			screen.setInvited(profile, !invited);
			title = checkbox(!invited).append(" " + player.name());
		}
	}
}
