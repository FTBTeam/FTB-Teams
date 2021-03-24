package dev.ftb.mods.ftbteams.client;

import com.feed_the_beast.mods.ftbguilibrary.config.ConfigGroup;
import com.feed_the_beast.mods.ftbguilibrary.config.gui.GuiEditConfig;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.widget.Button;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiBase;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiHelper;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiIcons;
import com.feed_the_beast.mods.ftbguilibrary.widget.SimpleButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbteams.data.FTBTUtils;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.net.MessageOpenGUIResponse;
import dev.ftb.mods.ftbteams.net.MessageUpdateSettings;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import dev.ftb.mods.ftbteams.property.TeamProperty;
import dev.ftb.mods.ftbteams.property.TeamPropertyValue;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Map;

public class MyTeamScreen extends GuiBase {
	public final MessageOpenGUIResponse data;
	public Button settingsButton;
	public Button colorButton;

	public MyTeamScreen(MessageOpenGUIResponse res) {
		data = res;
		setSize(300, 200);
	}

	@Override
	public void addWidgets() {
		add(settingsButton = new SimpleButton(this, new TranslatableComponent("gui.settings"), GuiIcons.SETTINGS.withTint(Color4I.BLACK), (simpleButton, mouseButton) -> {
			ConfigGroup config = new ConfigGroup("ftbteamsconfig");

			for (Map.Entry<TeamProperty, TeamPropertyValue> entry : data.properties.map.entrySet()) {
				entry.getKey().config(config, entry.getValue());
			}

			config.savedCallback = b -> {
				if (b) {
					new MessageUpdateSettings(data.properties).sendToServer();
				}

				openGui();
			};

			new GuiEditConfig(config).openGui();
		}));

		add(colorButton = new SimpleButton(this, new TranslatableComponent("gui.color"), data.properties.get(Team.COLOR).withBorder(Color4I.BLACK, false), (simpleButton, mouseButton) -> {
			Color4I c = FTBTUtils.randomColor();
			data.properties.set(Team.COLOR, c);
			simpleButton.setIcon(c.withBorder(Color4I.BLACK, false));
			TeamProperties properties = new TeamProperties();
			properties.set(Team.COLOR, c);
			new MessageUpdateSettings(properties).sendToServer();
		}) {
			@Override
			public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
				icon.draw(matrixStack, x, y, w, h);
			}
		});

		settingsButton.setPosAndSize(width - 19, 3, 16, 16);
		colorButton.setPosAndSize(5, 5, 12, 12);
	}

	@Override
	public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		GuiHelper.drawHollowRect(matrixStack, x, y, w, h, Color4I.BLACK, false);
		Color4I.WHITE.draw(matrixStack, x + 1, y + 1, w - 2, h - 2);
		Color4I.BLACK.draw(matrixStack, x + 1, y + 21, w - 2, 1);
		Color4I.BLACK.draw(matrixStack, x + 90, y + 22, 1, h - 23);
	}

	@Override
	public void drawForeground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.drawForeground(matrixStack, theme, x, y, w, h);
		theme.drawString(matrixStack, data.properties.get(Team.DISPLAY_NAME), x + w / 2F, y + 7, Color4I.BLACK, Theme.CENTERED);
	}
}
