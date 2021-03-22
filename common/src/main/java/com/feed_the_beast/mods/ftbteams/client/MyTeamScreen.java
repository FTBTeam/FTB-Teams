package com.feed_the_beast.mods.ftbteams.client;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiBase;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.feed_the_beast.mods.ftbteams.data.TeamMessage;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.List;

public class MyTeamScreen extends GuiBase {
	public final List<TeamMessage> messages;

	public MyTeamScreen(List<TeamMessage> m) {
		messages = m;
	}

	@Override
	public void addWidgets() {
	}

	@Override
	public void drawForeground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.drawForeground(matrixStack, theme, x, y, w, h);
		theme.drawString(matrixStack, "Not finished yet!", x + w / 2F, y + h / 2F - 9, Color4I.BLACK, Theme.CENTERED);
		theme.drawString(matrixStack, "Use /ftbteams sub-commands!", x + w / 2F, y + h / 2F + 4, Color4I.BLACK, Theme.CENTERED);
	}
}
