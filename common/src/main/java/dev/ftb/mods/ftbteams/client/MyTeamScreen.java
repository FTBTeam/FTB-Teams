package dev.ftb.mods.ftbteams.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbguilibrary.config.ConfigGroup;
import dev.ftb.mods.ftbguilibrary.config.gui.EditConfigScreen;
import dev.ftb.mods.ftbguilibrary.icon.Color4I;
import dev.ftb.mods.ftbguilibrary.widget.BaseScreen;
import dev.ftb.mods.ftbguilibrary.widget.Button;
import dev.ftb.mods.ftbguilibrary.widget.GuiHelper;
import dev.ftb.mods.ftbguilibrary.widget.GuiIcons;
import dev.ftb.mods.ftbguilibrary.widget.Panel;
import dev.ftb.mods.ftbguilibrary.widget.SimpleButton;
import dev.ftb.mods.ftbguilibrary.widget.Theme;
import dev.ftb.mods.ftbguilibrary.widget.Widget;
import dev.ftb.mods.ftbguilibrary.widget.WidgetLayout;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.FTBTUtils;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamRank;
import dev.ftb.mods.ftbteams.net.MessageOpenGUIResponse;
import dev.ftb.mods.ftbteams.net.MessageUpdateSettings;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import dev.ftb.mods.ftbteams.property.TeamProperty;
import dev.ftb.mods.ftbteams.property.TeamPropertyValue;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Map;
import java.util.UUID;

public class MyTeamScreen extends BaseScreen implements NordColors {
	public final MessageOpenGUIResponse data;
	public Button settingsButton;
	public Button colorButton;
	public Panel memberPanel;
	public Button inviteOrCreateParty;

	public MyTeamScreen(MessageOpenGUIResponse res) {
		data = res;
		setSize(300, 200);
	}

	@Override
	public void addWidgets() {
		add(settingsButton = new SimpleButton(this, new TranslatableComponent("gui.settings"), GuiIcons.SETTINGS.withTint(SNOW_STORM_2), (simpleButton, mouseButton) -> {
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

			new EditConfigScreen(config).openGui();
		}));

		add(colorButton = new SimpleButton(this, new TranslatableComponent("gui.color"), data.properties.get(Team.COLOR).withBorder(POLAR_NIGHT_0, false), (simpleButton, mouseButton) -> {
			Color4I c = FTBTUtils.randomColor();
			data.properties.set(Team.COLOR, c);
			simpleButton.setIcon(c.withBorder(POLAR_NIGHT_0, false));
			TeamProperties properties = new TeamProperties();
			properties.set(Team.COLOR, c);
			new MessageUpdateSettings(properties).sendToServer();
		}) {
			@Override
			public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
				icon.draw(matrixStack, x, y, w, h);
			}
		});

		add(memberPanel = new Panel(this) {
			@Override
			public void addWidgets() {
				for (Map.Entry<UUID, TeamRank> entry : ClientTeamManager.INSTANCE.selfTeam.getRanked(TeamRank.NONE).entrySet()) {
					add(new MemberButton(this, ClientTeamManager.INSTANCE.getProfile(entry.getKey()), entry.getValue()));
				}

				add(new AddMemberButton(this));
			}

			@Override
			public void alignWidgets() {
				align(new WidgetLayout.Vertical(1, 1, 1));

				for (Widget widget : widgets) {
					widget.setX(1);
					widget.setWidth(width - 2);
				}
			}
		});

		settingsButton.setPosAndSize(width - 19, 3, 16, 16);
		colorButton.setPosAndSize(5, 5, 12, 12);
		memberPanel.setPosAndSize(1, 22, 89, height - 24);
	}

	@Override
	public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		GuiHelper.drawHollowRect(matrixStack, x, y, w, h, POLAR_NIGHT_0, true);
		POLAR_NIGHT_1.draw(matrixStack, x + 1, y + 1, w - 2, h - 2);
		POLAR_NIGHT_0.draw(matrixStack, x + 1, y + 21, w - 2, 1);
		POLAR_NIGHT_0.draw(matrixStack, x + 90, y + 22, 1, h - 23);
	}

	@Override
	public void drawForeground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.drawForeground(matrixStack, theme, x, y, w, h);
		theme.drawString(matrixStack, data.properties.get(Team.DISPLAY_NAME), x + w / 2F, y + 7, SNOW_STORM_2, Theme.CENTERED);
	}
}
