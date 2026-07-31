package dev.ftb.mods.ftbteams.client.gui;

import dev.ftb.mods.ftblibrary.client.config.editable.EditableColor;
import dev.ftb.mods.ftblibrary.client.gui.input.Key;
import dev.ftb.mods.ftblibrary.client.gui.layout.WidgetLayout;
import dev.ftb.mods.ftblibrary.client.gui.theme.NordColors;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.*;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.platform.event.NativeEventPosting;
import dev.ftb.mods.ftblibrary.platform.network.Play2ServerNetworking;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamMessage;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.client.ClientTeamManager;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import dev.ftb.mods.ftbteams.api.event.TeamInfoEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import dev.ftb.mods.ftbteams.client.FTBTeamsClient;
import dev.ftb.mods.ftbteams.config.ServerConfig;
import dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl;
import dev.ftb.mods.ftbteams.data.PlayerPermissions;
import dev.ftb.mods.ftbteams.data.TeamPropertyCollectionImpl;
import dev.ftb.mods.ftbteams.data.TeamType;
import dev.ftb.mods.ftbteams.net.SendMessageMessage;
import dev.ftb.mods.ftbteams.net.ToggleChatRedirectionMessage;
import dev.ftb.mods.ftbteams.net.UpdatePropertiesRequestMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.text.DateFormat;
import java.util.*;

public class MyTeamScreen extends BaseScreen implements NordColors {
	private final TeamPropertyCollection properties;
	private final PlayerPermissions permissions;
	private final UUID teamID;
	private final Button settingsButton;
	private final Button infoButton;
	private final Button missingDataButton;
	private final Button colorButton;
	private final Button toggleChatButton;
	private final Button inviteButton;
	private final Button allyButton;
	private final Panel memberPanel;
	private final Panel chatPanel;
	private final TextBox chatBox;
	private final Button livesButton;

	// ranks which will appear in the member list, in descending order of seniority
	private static final List<TeamRank> PARTY_RANKS = List.of(TeamRank.OWNER, TeamRank.OFFICER, TeamRank.MEMBER, TeamRank.ALLY);

	private static final int MIN_MEMBER_PANEL_WIDTH = 80;

	public MyTeamScreen(PlayerPermissions permissions) {
		this.permissions = permissions;

		properties = getManager().selfTeam().getProperties();
		teamID = getManager().selfTeam().getId();

		settingsButton = new SettingsButton(this);
		infoButton = new SimpleButton(this, Component.empty(), Icons.INFO, (_, _) -> {}) {
			@Override
			public void addMouseOverText(TooltipList list) {
				addTeamInfo(list);
			}

			@Override
			public void playClickSound() {
			}
		};
		missingDataButton = new SimpleButton(this, Component.empty(), Icons.CANCEL, (_, _) -> {}) {
			@Override
			public void addMouseOverText(TooltipList list) {
				list.add(Component.translatable("ftbteams.missing_data").withStyle(ChatFormatting.RED));
			}

			@Override
			public void playClickSound() {
			}
		};
		colorButton = new ColorButton(this);
		toggleChatButton = new ToggleChatButton(this);
		inviteButton = new InviteButton(this);
		allyButton = new AllyButton(this);
		livesButton = new LivesButton();

		memberPanel = new MemberPanel();
		chatPanel = new ChatPanel();
		chatBox = new ChatBox();
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
		return FTBTeamsAPI.api().getClientManager();
	}

	@Override
	public boolean onInit() {
		setWidth(getWindow().getGuiScaledWidth() * 4 / 5);
		setHeight(getWindow().getGuiScaledHeight() * 4 / 5);
		return true;
	}

	@Override
	public void addWidgets() {
		add(settingsButton);
		add(infoButton);
		if (!getManager().isValid()) {
			add(missingDataButton);
		}
        if (ServerConfig.limitedLives().isPresent() && getManager().selfTeam().isPartyTeam()) {
			add(livesButton);
		}

		add(colorButton);

		add(toggleChatButton);
		add(inviteButton );
		add(allyButton );

		add(memberPanel);
		add(chatPanel);
		add(chatBox);
	}

	@Override
	public void alignWidgets() {
		super.alignWidgets();

		colorButton.setPosAndSize(5, 5, 12, 12);
		infoButton.setPosAndSize(20, 3, 16, 16);
		if (!getManager().isValid()) missingDataButton.setPosAndSize(40, 3, 16, 16);
        livesButton.setPosAndSize(40, 3, 16, 16);

		settingsButton.setPosAndSize(width - 19, 3, 16, 16);
		inviteButton.setPosAndSize(width - 37, 3, 16, 16);
		allyButton.setPosAndSize(width - 55, 3, 16, 16);
		toggleChatButton.setPosAndSize(width - 73, 3, 16, 16);

		memberPanel.setPosAndSize(1, 22, Math.max(memberPanel.width, MIN_MEMBER_PANEL_WIDTH), height - 23);
	}

	private void addTeamInfo(TooltipList list) {
		if (getManager().isValid()) {
			Team team = getManager().selfTeam();
			list.add(Component.translatable(team.getTypeTranslationKey()).withStyle(ChatFormatting.AQUA));
			list.add(Component.translatable("ftbteams.info.id", Component.literal(team.getId().toString()).withStyle(ChatFormatting.YELLOW)));
			list.add(Component.translatable("ftbteams.info.short_id", Component.literal(team.getShortName()).withStyle(ChatFormatting.YELLOW)));
			if (!team.getOwner().equals(Util.NIL_UUID)) {
				list.add(Component.translatable("ftbteams.info.owner", getManager().formatName(team.getOwner())));
			}

			NativeEventPosting.INSTANCE.postEvent(new TeamInfoEvent.Data(team, list::add));
		}
	}

	@Override
	public void drawBackground(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
		super.drawBackground(graphics, theme, x, y, w, h);
		IconHelper.renderIcon(POLAR_NIGHT_0, graphics, x, y + 21, w, 1);
		IconHelper.renderIcon(POLAR_NIGHT_0, graphics, x + memberPanel.width + 1, y + memberPanel.posY, 1, memberPanel.height + 1);
	}

	@Override
	public void drawForeground(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
		super.drawForeground(graphics, theme, x, y, w, h);
		theme.drawString(graphics, properties.get(TeamProperties.DISPLAY_NAME), x + w / 2, y + 7, SNOW_STORM_1, Theme.CENTERED);
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

	private class InviteButton extends SimpleButton {
		public InviteButton(Panel panel) {
			super(panel, Component.translatable("ftbteams.gui.invite"), Icons.ADD, (_, _) -> new InviteScreen().openGui());
		}

		@Override
		public boolean isEnabled() {
			if (ClientTeamManagerImpl.getInstance().selfTeam().getType() != TeamType.PARTY || !permissions.invitePlayer()) {
				return false;
			}
			KnownClientPlayer knownPlayer = ClientTeamManagerImpl.getInstance().self();
			return knownPlayer.online() && ClientTeamManagerImpl.getInstance().selfTeam().isOfficerOrBetter(knownPlayer.id());
		}

		@Override
		public boolean shouldDraw() {
			return isEnabled();
		}
	}

	private class AllyButton extends SimpleButton {
		public AllyButton(Panel panel) {
			super(panel, Component.translatable("ftbteams.gui.manage_allies"), Icons.FRIENDS, (_, _) -> new AllyScreen().openGui());
		}

		@Override
		public boolean isEnabled() {
			if (ClientTeamManagerImpl.getInstance().selfTeam().getType() != TeamType.PARTY || !permissions.addAlly()) {
				return false;
			}
			KnownClientPlayer knownPlayer = ClientTeamManagerImpl.getInstance().self();
			return knownPlayer.online() && ClientTeamManagerImpl.getInstance().selfTeam().isOfficerOrBetter(knownPlayer.id());
		}

		@Override
		public boolean shouldDraw() {
			return isEnabled();
		}
	}

	private static class ToggleChatButton extends SimpleButton {
		public ToggleChatButton(Panel panel) {
			super(panel, Component.translatable("ftbteams.gui.toggle_chat"), Icons.CHAT,
					(_, _) -> Play2ServerNetworking.send(ToggleChatRedirectionMessage.INSTANCE)
			);
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			list.add(Component.translatable("ftbteams.message.chat_redirected." + (FTBTeamsClient.isChatRedirected() ? "on" : "off")));
			list.add(Component.translatable("ftbteams.gui.toggle_chat").withStyle(ChatFormatting.GRAY));
		}

		@Override
		public void tick() {
			setIcon(FTBTeamsClient.isChatRedirected() ? Icons.CHAT.withTint(Color4I.rgb(0xFFA060)) : Icons.CHAT);
		}

		@Override
		public boolean isEnabled() {
			return ClientTeamManagerImpl.getInstance().selfTeam().getType() == TeamType.PARTY;
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
			if (!manager.isValid()) {
				return;
			}

			for (TeamMessage message : manager.selfTeam().getMessageHistory()) {
				if (!message.sender().equals(prev)) {
					add(new VerticalSpaceWidget(this, 2));

					Component name = manager.formatName(message.sender()).copy().append(":");
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
		public void drawBackground(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
			IconHelper.renderIcon(NordColors.POLAR_NIGHT_2, graphics, x, y, w, h);
		}
	}

	private class ChatBox extends TextBox {
		public ChatBox() {
			super(MyTeamScreen.this);
		}

		@Override
		public void drawTextBox(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
			IconHelper.renderIcon(NordColors.POLAR_NIGHT_3, graphics, x, y, w, h);
		}

		@Override
		public void onEnterPressed() {
			Play2ServerNetworking.send(new SendMessageMessage(getText()));
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
			if (!manager.isValid()) {
				return;
			}

			Map<TeamRank, List<KnownClientPlayer>> byRank = new EnumMap<>(TeamRank.class);
			manager.selfTeam().getPlayersByRank(TeamRank.NONE).forEach((id, rank) ->
					manager.getKnownPlayer(id).ifPresent(kcp -> byRank.computeIfAbsent(rank, _ -> new ArrayList<>()).add(kcp)));

			for (TeamRank rank : PARTY_RANKS) {
				byRank.getOrDefault(rank, List.of()).stream()
						.sorted(Comparator.comparing(KnownClientPlayer::name))
						.map(kcp -> new MemberButton(this, kcp))
						.forEach(this::add);
			}

			if (manager.selfTeam().isPlayerTeam()) {
				add(new CreatePartyButton(this, permissions));
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

	private static class SettingsButton extends SimpleButton {
		public SettingsButton(MyTeamScreen myTeamScreen) {
			super(myTeamScreen, Component.translatable("gui.settings"), Icons.SETTINGS.withTint(NordColors.SNOW_STORM_2),
					(_, _) -> FTBTeamsClient.openTeamSettingsScreen(_ -> myTeamScreen.openGui()));
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
			drawIcon(graphics, theme, x, y, w, h);
		}
	}

	private class ColorButton extends SimpleButton {
		public ColorButton(MyTeamScreen screen) {
			super(MyTeamScreen.this, Component.translatable("gui.color"),
					screen.properties.get(TeamProperties.COLOR).withBorder(NordColors.POLAR_NIGHT_0, false),
					(b, _) -> onClick(screen, b)
			);
		}

		private static void onClick(MyTeamScreen screen, SimpleButton simpleButton) {
			EditableColor config = new EditableColor();
			config.setValue(screen.properties.get(TeamProperties.COLOR));
			ColorSelectorPanel.popupAtMouse(screen.getGui(), config, accepted -> {
				if (accepted) {
					Color4I c = config.getValue();
					screen.properties.set(TeamProperties.COLOR, c);
					simpleButton.setIcon(c.withBorder(NordColors.POLAR_NIGHT_0, false));
					TeamPropertyCollection properties = new TeamPropertyCollectionImpl();
					properties.set(TeamProperties.COLOR, c);
					Play2ServerNetworking.send(new UpdatePropertiesRequestMessage(properties));
				}
			});
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
			drawIcon(graphics, theme, x, y, w, h);
		}
	}

	private class LivesButton extends SimpleButton {
		public LivesButton() {
			super(MyTeamScreen.this, Component.empty(), Icons.HEART, (_, _) -> {});
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			FTBTeamsClient.addLivesIconTooltip().forEach(list::add);
		}

		@Override
		public void drawIcon(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
			super.drawIcon(graphics, theme, x, y, w, h);

			graphics.pose().pushMatrix();
			graphics.pose().translate(x, y);
			FTBTeamsClient.renderLivesIconOverlay(graphics, theme.getFont(), w);
			graphics.pose().popMatrix();
		}
	}
}
