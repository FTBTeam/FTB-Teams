package com.feed_the_beast.mods.ftbteams.client;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiBase;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiHelper;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiIcons;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.feed_the_beast.mods.ftbteams.net.MessageOpenGUIResponse;
import com.mojang.blaze3d.vertex.PoseStack;

public class MyTeamScreen extends GuiBase {
	public final MessageOpenGUIResponse data;

	public MyTeamScreen(MessageOpenGUIResponse res) {
		data = res;
		setSize(300, 200);
	}

	@Override
	public void addWidgets() {
	}

	@Override
	public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		GuiHelper.drawHollowRect(matrixStack, x, y, w, h, Color4I.BLACK, false);
		Color4I.WHITE.draw(matrixStack, x + 1, y + 1, w - 2, h - 2);
		Color4I.BLACK.draw(matrixStack, x + 1, y + 21, w - 2, 1);
		Color4I.BLACK.draw(matrixStack, x + 90, y + 22, 1, h - 23);
		GuiIcons.SETTINGS.withTint(Color4I.BLACK).draw(matrixStack, x + w - 19, y + 3, 16, 16);
	}

	@Override
	public void drawForeground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.drawForeground(matrixStack, theme, x, y, w, h);
		theme.drawString(matrixStack, data.displayName, x + w / 2F, y + 7, Color4I.BLACK, Theme.CENTERED);
	}
}
