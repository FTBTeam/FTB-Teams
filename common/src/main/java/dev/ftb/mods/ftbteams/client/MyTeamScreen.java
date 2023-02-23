package dev.ftb.mods.ftbteams.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.misc.NordColors;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.data.*;
import dev.ftb.mods.ftbteams.net.SendMessageMessage;
import dev.ftb.mods.ftbteams.net.UpdateSettingsMessage;
import dev.ftb.mods.ftbteams.property.TeamProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.text.DateFormat;
import java.util.*;

public class MyTeamScreen extends BaseScreen implements NordColors {
	private final TeamProperties properties;
	private final UUID teamID;
	private Button settingsButton;
	private Button infoButton;
	private Button missingDataButton;
	private Button colorButton;
	private Button inviteButton;
	private Button allyButton;
	private Panel memberPanel;
	private Panel chatPanel;
	private TextBox chatBox;

	// ranks which will appear in the member list, in descending order of seniority
	private static final List<TeamRank> PARTY_RANKS = List.of(TeamRank.OWNER, TeamRank.OFFICER, TeamRank.MEMBER, TeamRank.ALLY);

	private static final int MIN_MEMBER_PANEL_WIDTH = 80;

	public MyTeamScreen(TeamProperties props) {
		properties = props;
		teamID = getManager().selfTeam().getId();
	}

	public static void refreshIfOpen() {
		if (Minecraft.getInstance().screen instanceof ScreenWrapper w && w.getGui() instanceof MyTeamScreen mts) {
			if (mts.getManager().selfTeam().getId().equals(mts.teamID)) {
				mts.refreshWidgets();
			} else {
				// team has changed (player left or got kicked from party?)
				mts.closeGui(false);
			}
		}
	}

	private ClientTeamManager getManager() {
		return ClientTeamManager.INSTANCE;
	}

	@Override
	public boolean onInit() {
		setWidth(getScreen().getGuiScaledWidth() * 4 / 5);
		setHeight(getScreen().getGuiScaledHeight() * 4 / 5);
		return true;
	}

	@Override
	public void addWidgets() {

		add(settingsButton = new SettingsButton());

		add(infoButton = new SimpleButton(this, Component.empty(), Icons.INFO, (w,mb) -> {}) {
			@Override
			public void addMouseOverText(TooltipList list) {
				addTeamInfo(list);
			}

			@Override
			public void playClickSound() {
			}
		});

		if (ClientTeamManager.INSTANCE.self() == null) {
			add(missingDataButton = new SimpleButton(this, Component.empty(), Icons.CLOSE, (w, mb) -> {}) {
				@Override
				public void addMouseOverText(TooltipList list) {
					list.add(Component.translatable("ftbteams.missing_data").withStyle(ChatFormatting.RED));
				}

				@Override
				public void playClickSound() {
				}
			});
		}

		add(colorButton = new SimpleButton(this, Component.translatable("gui.color"), properties.get(Team.COLOR).withBorder(POLAR_NIGHT_0, false), (simpleButton, mouseButton) -> {
			Color4I c = FTBTUtils.randomColor();
			properties.set(Team.COLOR, c);
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

		add(inviteButton = new InviteButton(this));
		add(allyButton = new AllyButton(this));

		add(memberPanel = new MemberPanel());
		add(chatPanel = new ChatPanel());
		add(chatBox = new ChatBox());
	}

	@Override
	public void alignWidgets() {
		super.alignWidgets();
		
		colorButton.setPosAndSize(5, 5, 12, 12);
		infoButton.setPosAndSize(20, 3, 16, 16);
		if (missingDataButton != null) missingDataButton.setPosAndSize(40, 3, 16, 16);

		settingsButton.setPosAndSize(width - 19, 3, 16, 16);
		inviteButton.setPosAndSize(width - 37, 3, 16, 16);
		allyButton.setPosAndSize(width - 55, 3, 16, 16);

		memberPanel.setPosAndSize(1, 22, Math.max(memberPanel.width, MIN_MEMBER_PANEL_WIDTH), height - 23);
	}

	private void addTeamInfo(TooltipList list) {
		ClientTeamManager manager = getManager();
		if (manager != null) {
			ClientTeam team = getManager().selfTeam();
			list.add(Component.translatable("ftbteams.team_type." + team.getType().getSerializedName()).withStyle(ChatFormatting.AQUA));
			list.add(Component.translatable("ftbteams.info.id", Component.literal(team.getId().toString()).withStyle(ChatFormatting.YELLOW)));
			list.add(Component.translatable("ftbteams.info.short_id", Component.literal(team.getStringID()).withStyle(ChatFormatting.YELLOW)));
			if (!team.getOwnerID().equals(Util.NIL_UUID)) {
				list.add(Component.translatable("ftbteams.info.owner", getManager().getName(team.getOwnerID())));
			}
		}
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
		theme.drawString(matrixStack, properties.get(Team.DISPLAY_NAME), x + w / 2F, y + 7, SNOW_STORM_1, Theme.CENTERED);
	}

	@Override
	public boolean keyPressed(Key key) {
		if (key.is(GLFW.GLFW_KEY_TAB)) {
			chatBox.setFocused(true);
			return true;
		}
		return super.keyPressed(key);
	}

	public void refreshChat() {
		chatPanel.refreshWidgets();
	}

	private static class InviteButton extends SimpleButton {
		public InviteButton(Panel panel) {
			super(panel, Component.translatable("ftbteams.gui.invite"), Icons.ADD, (w, mb) -> new InviteScreen().openGui());
		}

		@Override
		public boolean isEnabled() {
			if (ClientTeamManager.INSTANCE.selfTeam().getType() != TeamType.PARTY) {
				return false;
			}
			KnownClientPlayer knownPlayer = ClientTeamManager.INSTANCE.self();
			return knownPlayer != null && ClientTeamManager.INSTANCE.selfTeam().isOfficer(knownPlayer.id());
		}

		@Override
		public boolean shouldDraw() {
			return isEnabled();
		}
	}


	private static class AllyButton extends SimpleButton {
		public AllyButton(Panel panel) {
			super(panel, Component.translatable("ftbteams.gui.manage_allies"), Icons.FRIENDS, (w, mb) -> new AllyScreen().openGui());
		}

		@Override
		public boolean isEnabled() {
			if (ClientTeamManager.INSTANCE.selfTeam().getType() != TeamType.PARTY) {
				return false;
			}
			KnownClientPlayer knownPlayer = ClientTeamManager.INSTANCE.self();
			return knownPlayer != null && ClientTeamManager.INSTANCE.selfTeam().isOfficer(knownPlayer.id());
		}

		@Override
		public boolean shouldDraw() {
			return isEnabled();
		}
	}

	private class ChatPanel extends Panel {
		public ChatPanel() {
			super(MyTeamScreen.this);
		}

		@Override
		public void addWidgets() {
			UUID prev = null;

			ClientTeamManager manager = getManager();
			if (manager == null) return;

			for (TeamMessage message : manager.selfTeam().getMessageHistory()) {
				if (!message.sender().equals(prev)) {
					add(new VerticalSpaceWidget(this, 2));

					Component name = manager.getName(message.sender()).copy().append(":");
					add(new TextField(this).setMaxWidth(width).setText(name));

					prev = message.sender();
				}

				add(new TextField(this) {
					@Override
					public void addMouseOverText(TooltipList list) {
						list.add(Component.literal(DateFormat.getInstance().format(new Date(message.date()))));
					}
				}.setMaxWidth(width).setText(Component.literal("  ").append(message.text())));
			}

			if (!widgets.isEmpty()) {
				add(new TextField(this).setMaxWidth(width).setText(Component.empty()));
			}
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
	}

	private class ChatBox extends TextBox {
		public ChatBox() {
			super(MyTeamScreen.this);
		}

		@Override
		public void drawTextBox(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
			NordColors.POLAR_NIGHT_3.draw(matrixStack, x, y, w, h);
		}

		@Override
		public void onEnterPressed() {
			new SendMessageMessage(getText()).sendToServer();
			setText("");
			setFocused(true);
		}
	}

	private class MemberPanel extends Panel {
		public MemberPanel() {
			super(MyTeamScreen.this);
		}

		@Override
		public void addWidgets() {
			ClientTeamManager manager = getManager();
			if (manager == null || manager.isInvalid()) return;

			PARTY_RANKS.stream()
					.flatMap(rank -> manager.selfTeam().getRanked(rank).entrySet().stream()
							.filter(e -> e.getValue() == rank)
							.map(e -> manager.getKnownPlayer(e.getKey()))
							.filter(Objects::nonNull)
							.sorted(Comparator.comparing(KnownClientPlayer::name))
							.map(kcp -> new MemberButton(this, kcp))
					).forEach(this::add);

			if (manager.selfTeam().getType() == TeamType.PLAYER) {
				add(new CreatePartyButton(this));
			}
		}

		@Override
		public void alignWidgets() {
			align(new WidgetLayout.Vertical(1, 2, 1));

			width = MIN_MEMBER_PANEL_WIDTH;
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
	}

	private class SettingsButton extends SimpleButton {
		public SettingsButton() {
			super(MyTeamScreen.this, Component.translatable("gui.settings"), Icons.SETTINGS.withTint(NordColors.SNOW_STORM_2), (simpleButton, mouseButton) -> {
				ConfigGroup config = new ConfigGroup("ftbteamsconfig", accepted -> {
					if (accepted) {
						new UpdateSettingsMessage(MyTeamScreen.this.properties).sendToServer();
					}
					MyTeamScreen.this.openGui();
				});

				Map<String,ConfigGroup> subGroups = new HashMap<>();
				MyTeamScreen.this.properties.forEach((key, value) -> {
					String groupName = key.getId().getNamespace();
					ConfigGroup cfg = subGroups.computeIfAbsent(groupName, k -> config.getOrCreateSubgroup(groupName));
					key.config(cfg, value);
				});

				new EditConfigScreen(config).openGui();
			});
		}

		@Override
		public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
			drawIcon(matrixStack, theme, x, y, w, h);
		}
	}
}
