package dev.ftb.mods.ftbteams.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.ftb.mods.ftblibrary.api.sidebar.SidebarButtonCreatedEvent;
import dev.ftb.mods.ftblibrary.client.gui.CustomClickEvent;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamMessage;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import dev.ftb.mods.ftbteams.client.gui.MyTeamScreen;
import dev.ftb.mods.ftbteams.config.ServerConfig;
import dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl;
import dev.ftb.mods.ftbteams.data.PlayerPermissions;
import dev.ftb.mods.ftbteams.net.OpenGUIMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FTBTeamsClient {
	public static final Identifier OPEN_GUI_ID = FTBTeamsAPI.id("open_gui");
	public static final Identifier TEAM_LIVES_ID = FTBTeamsAPI.id("team_lives");
	public static final KeyMapping.Category FTBTEAMS_KEY_CATEGORY = new KeyMapping.Category(FTBTeamsAPI.id("ftbteams_key"));

	public static final KeyMapping openTeamsKey
			= new KeyMapping("key.ftbteams.open_gui", InputConstants.Type.KEYSYM, -1, FTBTEAMS_KEY_CATEGORY);
	private static boolean chatRedirected = false;

	public static void init() {
		registerKeys();

		CustomClickEvent.EVENT.register(event -> {
			if (event.id().equals(OPEN_GUI_ID)) {
				OpenGUIMessage.sendToServer();
				return EventResult.interruptTrue();
			}
			return EventResult.pass();
		});

		ClientRawInputEvent.KEY_PRESSED.register(FTBTeamsClient::keyPressed);

		SidebarButtonCreatedEvent.EVENT.register(FTBTeamsClient::onSidebarButtonCreated);
	}

	private static void onSidebarButtonCreated(SidebarButtonCreatedEvent event) {
		if (event.getButton().getId().equals(TEAM_LIVES_ID)) {
			event.getButton().addVisibilityCondition(() -> ServerConfig.limitedLives().isPresent() && FTBTeamsAPI.api().getClientManager().selfTeam().isPartyTeam());
			event.getButton().setTooltipOverride(FTBTeamsClient::addLivesIconTooltip);
			event.getButton().addOverlayRender(FTBTeamsClient::renderLivesIconOverlay);
		}
	}

	private static void registerKeys() {
		KeyMappingRegistry.register(openTeamsKey);
	}

	private static EventResult keyPressed(Minecraft client, int keycode, KeyEvent event) {
		if (openTeamsKey.isDown()) {
			OpenGUIMessage.sendToServer();
			return EventResult.interruptTrue();
		}

		return EventResult.pass();
	}

	public static void openMyTeamGui(TeamPropertyCollection properties, PlayerPermissions permissions) {
		new MyTeamScreen(properties, permissions).openGui();
	}

	public static void updateSettings(UUID id, TeamPropertyCollection properties) {
		ClientTeamManagerImpl.ifPresent(mgr -> mgr.getTeam(id).ifPresent(team -> team.updateProperties(properties)));
	}

	public static void sendMessage(UUID from, Component text) {
		ClientTeamManagerImpl.ifPresent(mgr -> {
			TeamMessage msg = FTBTeamsAPI.api().createMessage(from, text);
			mgr.selfTeam().addMessage(msg);

			MyTeamScreen screen = ClientUtils.getCurrentGuiAs(MyTeamScreen.class);
			if (screen != null) {
				screen.refreshChat();
			}
		});
	}

	public static void updatePresence(KnownClientPlayer update) {
		ClientTeamManagerImpl.ifPresent(mgr -> mgr.updatePresence(update));
	}

	public static void setChatRedirected(boolean chatRedirected) {
		FTBTeamsClient.chatRedirected = chatRedirected;
	}

	public static boolean isChatRedirected() {
		return chatRedirected;
	}

	public static List<Component> addLivesIconTooltip() {
		Team team = FTBTeamsAPI.api().getClientManager().selfTeam();
		List<Component> res = new ArrayList<>();
		int lives = team.getProperty(TeamProperties.LIVES_REMAINING);
		res.add(Component.translatable("ftbteams.message.limited_lives", lives, ServerConfig.LIMITED_LIVES.get()));
		if (lives == 0) {
			res.add(Component.translatable("ftbteams.message.limited_lives.warn").withStyle(ChatFormatting.RED));
		}
		return res;
	}

	public static void renderLivesIconOverlay(GuiGraphics graphics, Font font, int buttonSize) {
		String text = String.valueOf(FTBTeamsAPI.api().getClientManager().selfTeam().getProperty(TeamProperties.LIVES_REMAINING));
		if (!text.isEmpty()) {
			var nw = font.width(text);
			graphics.pose().pushMatrix();
			graphics.pose().translate(buttonSize - nw, buttonSize - font.lineHeight);
			graphics.pose().scale(0.75f, 0.75f);
			IconHelper.renderIcon(Color4I.rgb(0xFF208020), graphics, 0, 0, nw + 1, font.lineHeight);
			graphics.drawString(font, text, 1, 1, 0xFFFFFFFF);
			graphics.pose().popMatrix();
		}
	}
}
