package dev.ftb.mods.ftbteams.client;

import dev.ftb.mods.ftblibrary.api.event.client.SidebarButtonCreatedEvent;
import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.client.config.gui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.platform.client.PlatformClient;
import dev.ftb.mods.ftblibrary.platform.client.input.InputHelper;
import dev.ftb.mods.ftblibrary.platform.network.Play2ServerNetworking;
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
import dev.ftb.mods.ftbteams.net.UpdatePropertiesRequestMessage;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;

public class FTBTeamsClient {
	public static final Identifier OPEN_GUI_ID = FTBTeamsAPI.id("open_gui");
	public static final Identifier TEAM_LIVES_ID = FTBTeamsAPI.id("team_lives");

	public static final KeyMapping.Category FTBTEAMS_KEY_CATEGORY = new KeyMapping.Category(FTBTeamsAPI.id("default"));
	public static final KeyMapping openTeamsKey = InputHelper.createSimpleKeyMapping("open_gui", FTBTEAMS_KEY_CATEGORY);

	private static boolean chatRedirected = false;

	public static void init() {
		registerKeys();
	}

	public static void onSidebarButtonCreated(SidebarButtonCreatedEvent.Data event) {
		if (event.button().getId().equals(TEAM_LIVES_ID)) {
			event.button().addVisibilityCondition(() -> ServerConfig.limitedLives().isPresent() && FTBTeamsAPI.api().getClientManager().selfTeam().isPartyTeam());
			event.button().setTooltipOverride(FTBTeamsClient::addLivesIconTooltip);
			event.button().addOverlayRender(FTBTeamsClient::renderLivesIconOverlay);
		}
	}

	private static void registerKeys() {
		PlatformClient.get().input().registerKeyMapping(FTBTeamsAPI.MOD_ID, openTeamsKey);
	}

	public static void keyPressed(Minecraft ignoredClient) {
		if (openTeamsKey.isDown()) {
			OpenGUIMessage.sendToServer();
		}
	}

	public static void openMyTeamGui(PlayerPermissions permissions) {
		new MyTeamScreen(permissions).openGui();
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

	public static void renderLivesIconOverlay(GuiGraphicsExtractor graphics, Font font, int buttonSize) {
		String text = String.valueOf(FTBTeamsAPI.api().getClientManager().selfTeam().getProperty(TeamProperties.LIVES_REMAINING));
		if (!text.isEmpty()) {
			var nw = font.width(text);
			graphics.pose().pushMatrix();
			graphics.pose().translate(buttonSize - nw, buttonSize - font.lineHeight);
			graphics.pose().scale(0.75f, 0.75f);
			IconHelper.renderIcon(Color4I.rgb(0xFF208020), graphics, 0, 0, nw + 1, font.lineHeight);
			graphics.text(font, text, 1, 1, 0xFFFFFFFF);
			graphics.pose().popMatrix();
		}
	}

	public static void openTeamSettingsScreen(BooleanConsumer onFinished) {
		var props = ClientTeamManagerImpl.getInstance().selfTeam().getProperties();

		EditableConfigGroup config = new EditableConfigGroup("ftbteamsconfig", accepted -> {
			if (accepted) {
				Play2ServerNetworking.send(new UpdatePropertiesRequestMessage(props));
			}
			onFinished.accept(accepted);
		});

		Map<String,EditableConfigGroup> subGroups = new HashMap<>();
		props.forEach((key, value) -> {
			if (!key.isHidden()) {
				String groupName = key.getId().getNamespace();
				EditableConfigGroup cfg = subGroups.computeIfAbsent(groupName, _ -> config.getOrCreateSubgroup(groupName));
				var val = key.config(cfg, value);
				if (val != null && !key.isPlayerEditable()) {
					val.setCanEdit(false);
				}
			}
		});

		new EditConfigScreen(config).openGui();
	}
}
