package dev.ftb.mods.ftbteams.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.misc.NordColors;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.FTBTUtils;
import dev.ftb.mods.ftbteams.data.KnownClientPlayer;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamMessage;
import dev.ftb.mods.ftbteams.data.TeamRank;
import dev.ftb.mods.ftbteams.data.TeamType;
import dev.ftb.mods.ftbteams.net.OpenMyTeamGUIMessage;
import dev.ftb.mods.ftbteams.net.SendMessageMessage;
import dev.ftb.mods.ftbteams.net.UpdateSettingsMessage;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import dev.ftb.mods.ftbteams.property.TeamProperty;
import dev.ftb.mods.ftbteams.property.TeamPropertyValue;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MyTeamScreen extends BaseScreen implements NordColors {
	public final ClientTeamManager manager;
	public final OpenMyTeamGUIMessage data;
	public Button settingsButton;
	public Button colorButton;
	public Panel memberPanel;
	public Panel chatPanel;
	public TextBox chatBox;

	public MyTeamScreen(OpenMyTeamGUIMessage res) {
		manager = Objects.requireNonNull(ClientTeamManager.INSTANCE);
		data = res;
		setSize(300, 200);
	}

	@Override
	public void addWidgets() {
		add(settingsButton = new SimpleButton(this, new TranslatableComponent("gui.settings"), Icon.getIcon("ftbteams:textures/settings.png").withTint(SNOW_STORM_2), (simpleButton, mouseButton) -> {
			ConfigGroup config = new ConfigGroup("ftbteamsconfig");

			for (Map.Entry<TeamProperty, TeamPropertyValue> entry : data.properties.map.entrySet()) {
				entry.getKey().config(config, entry.getValue());
			}

			config.savedCallback = b -> {
				if (b) {
					new UpdateSettingsMessage(data.properties).sendToServer();
				}

				openGui();
			};

			new EditConfigScreen(config).openGui();
		}) {
			@Override
			public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
				drawIcon(matrixStack, theme, x, y, w, h);
			}
		});

		add(colorButton = new SimpleButton(this, new TranslatableComponent("gui.color"), data.properties.get(Team.COLOR).withBorder(POLAR_NIGHT_0, false), (simpleButton, mouseButton) -> {
			Color4I c = FTBTUtils.randomColor();
			data.properties.set(Team.COLOR, c);
			simpleButton.setIcon(c.withBorder(POLAR_NIGHT_0, false));
			TeamProperties properties = new TeamProperties();
			properties.set(Team.COLOR, c);
			new UpdateSettingsMessage(properties).sendToServer();
		}) {
			@Override
			public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
				icon.draw(matrixStack, x, y, w, h);
			}
		});

		add(memberPanel = new Panel(this) {
			@Override
			public void addWidgets() {
				for (Map.Entry<UUID, TeamRank> entry : manager.selfTeam.getRanked(TeamRank.NONE).entrySet()) {
					KnownClientPlayer p = manager.getKnownPlayer(entry.getKey());

					if (p != null) {
						add(new MemberButton(this, p, entry.getValue()));
					}
				}

				if (manager.selfTeam.getType() == TeamType.PLAYER) {
					add(new CreatePartyButton(this));
				}

				/*
				add(new NordButton(this, new TextComponent("Add Ally"), Icon.getIcon(FTBTeams.MOD_ID + ":textures/add.png")) {
					@Override
					public void onClicked(MouseButton mouseButton) {
					}
				});

				add(new NordButton(this, new TextComponent("Add Member"), Icon.getIcon(FTBTeams.MOD_ID + ":textures/add.png")) {
					@Override
					public void onClicked(MouseButton mouseButton) {
					}
				});
				 */
			}

			@Override
			public void alignWidgets() {
				align(new WidgetLayout.Vertical(1, 2, 1));

				width = 80;

				for (Widget widget : widgets) {
					width = Math.max(width, widget.width);
				}

				for (Widget widget : widgets) {
					widget.setX(1);
					widget.setWidth(width - 2);
				}

				chatPanel.setPosAndSize(width + 3, 23, MyTeamScreen.this.width - memberPanel.width - 5, MyTeamScreen.this.height - 40);
				chatBox.setPosAndSize(chatPanel.posX, MyTeamScreen.this.height - 15, chatPanel.width, 13);
			}
		});

		add(chatPanel = new Panel(this) {
			@Override
			public void addWidgets() {
				UUID prev = null;

				for (TeamMessage message : manager.selfTeam.messageHistory) {
					if (!message.sender.equals(prev)) {
						TextComponent name = new TextComponent("");
						name.append(manager.getName(message.sender));
						name.append(":");

						add(new TextField(this).setMaxWidth(width).setText(name));
						prev = message.sender;
					}

					add(new TextField(this).setMaxWidth(width).setText(new TextComponent("  ").append(message.text)));
				}

				if (!widgets.isEmpty()) {
					add(new TextField(this).setMaxWidth(width).setText(TextComponent.EMPTY));
				}

				add(new TextField(this).setMaxWidth(width).setText(new TextComponent("This UI is WIP! Use /ftbteams for now!")));
			}

			@Override
			public void alignWidgets() {
				align(new WidgetLayout.Vertical(2, 1, 1));
				movePanelScroll(0, getContentHeight());
			}

			@Override
			public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
				NordColors.POLAR_NIGHT_2.draw(matrixStack, x, y, w, h);
			}
		});

		add(chatBox = new TextBox(this) {
			@Override
			public void drawTextBox(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
				NordColors.POLAR_NIGHT_2.draw(matrixStack, x, y, w, h);
			}

			@Override
			public void onEnterPressed() {
				new SendMessageMessage(getText()).sendToServer();
				setText("");
				setFocused(true);
			}
		});

		settingsButton.setPosAndSize(width - 19, 4, 14, 14);
		colorButton.setPosAndSize(5, 5, 12, 12);
		memberPanel.setPosAndSize(1, 22, 89, height - 23);
	}

	@Override
	public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		GuiHelper.drawHollowRect(matrixStack, x, y, w, h, POLAR_NIGHT_0, true);
		POLAR_NIGHT_1.draw(matrixStack, x + 1, y + 1, w - 2, h - 2);
		POLAR_NIGHT_0.draw(matrixStack, x + 1, y + 21, w - 2, 1);
		POLAR_NIGHT_0.draw(matrixStack, x + memberPanel.width + 1, y + memberPanel.posY, 1, memberPanel.height);
	}

	@Override
	public void drawForeground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.drawForeground(matrixStack, theme, x, y, w, h);
		theme.drawString(matrixStack, data.properties.get(Team.DISPLAY_NAME), x + w / 2F, y + 7, SNOW_STORM_1, Theme.CENTERED);
	}

	@Override
	public boolean keyPressed(Key key) {
		if (key.escOrInventory()) {
			closeGui(false);
			return true;
		}
		return false;
	}
}
