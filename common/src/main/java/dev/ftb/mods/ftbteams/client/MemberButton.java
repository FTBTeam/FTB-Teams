package dev.ftb.mods.ftbteams.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbteams.data.*;
import dev.ftb.mods.ftbteams.net.PlayerGUIOperationMessage.Operation;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MemberButton extends NordButton {
	public final KnownClientPlayer player;
	public final TeamRank rank;


	MemberButton(Panel panel, KnownClientPlayer p, TeamRank r) {
		super(panel, Component.literal(p.name), FaceIcon.getFace(p.getProfile()));
		setWidth(width + 18);  // to fit in the rank icon
		player = p;
		rank = r;
	}

	@Override
	public void drawIcon(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.drawIcon(matrixStack, theme, x, y, w, h);

		if (player.online) {
			matrixStack.pushPose();
			matrixStack.translate(x + w - 1.5D, y - 0.5D, 0);
			Color4I.GREEN.draw(matrixStack, 0, 0, 2, 2);
			matrixStack.popPose();
		}
		if (ClientTeamManager.INSTANCE.selfTeam.getType() == TeamType.PARTY) {
			TeamRank tr = ClientTeamManager.INSTANCE.selfTeam.getHighestRank(player.uuid);
			tr.getIcon().ifPresent(icon -> icon.draw(matrixStack, getX() + width - 14, getY() + 2, 12, 12));
		}
	}

	@Override
	public void onClicked(MouseButton button) {
		if (ClientTeamManager.INSTANCE.selfKnownPlayer == null || ClientTeamManager.INSTANCE.selfTeam == null) return;

		KnownClientPlayer self = ClientTeamManager.INSTANCE.selfKnownPlayer;
		ClientTeam selfTeam = ClientTeamManager.INSTANCE.selfTeam;
		TeamRank selfRank = selfTeam.getHighestRank(self.uuid);
		TeamRank playerRank = selfTeam.getHighestRank(player.uuid);

		if (selfTeam.getType() != TeamType.PARTY) return;

		List<ContextMenuItem> items0 = new ArrayList<>();
		if (player.uuid.equals(self.uuid)) {
			if (selfRank.is(TeamRank.OWNER)) {
				if (selfTeam.getMembers().size() == 1) {
					items0.add(new ContextMenuItem(Component.translatable("ftbteams.gui.disband"), Icons.CLOSE,
							() -> Operation.LEAVE.sendMessage(player))
							.setYesNo(Component.translatable("ftbteams.gui.disband.confirm")));
				}
			} else {
				items0.add(new ContextMenuItem(Component.translatable("ftbteams.gui.leave"), Icons.CLOSE,
						() -> Operation.LEAVE.sendMessage(player))
						.setYesNo(Component.translatable("ftbteams.gui.leave.confirm")));
			}
		} else {
			if (selfRank.is(TeamRank.OWNER)) {
				if (playerRank == TeamRank.MEMBER) {
					items0.add(new ContextMenuItem(Component.translatable("ftbteams.gui.promote", player.name), Icons.SHIELD,
							() -> Operation.PROMOTE.sendMessage(player))
							.setYesNo(Component.translatable("ftbteams.gui.promote.confirm", player.name)));
				} else if (playerRank == TeamRank.OFFICER) {
					items0.add(new ContextMenuItem(Component.translatable("ftbteams.gui.demote", player.name), Icons.ACCEPT_GRAY,
							() -> Operation.DEMOTE.sendMessage(player))
							.setYesNo(Component.translatable("ftbteams.gui.demote.confirm", player.name)));
				}
				if (playerRank.isMember()) {
					items0.add(new ContextMenuItem(Component.translatable("ftbteams.gui.transfer_ownership", player.name), Icons.DIAMOND,
							() -> Operation.TRANSFER_OWNER.sendMessage(player))
							.setYesNo(Component.translatable("ftbteams.gui.transfer_ownership.confirm", player.name)));
				}
			}
		}
		if (selfRank.getPower() > playerRank.getPower()) {
			if (playerRank.isMember()) {
				items0.add(new ContextMenuItem(Component.translatable("ftbteams.gui.kick", player.name), Icons.CLOSE,
						() -> Operation.KICK.sendMessage(player))
						.setYesNo(Component.translatable("ftbteams.gui.kick.confirm", player.name)));
			} else if (selfRank.isOfficer() && playerRank.isAlly()) {
				items0.add(new ContextMenuItem(Component.translatable("ftbteams.gui.remove_ally", player.name), Icons.CANCEL,
						() -> Operation.REMOVE_ALLY.sendMessage(player))
						.setYesNo(Component.translatable("ftbteams.gui.remove_ally.confirm", player.name)));
				}
		}

		if (!items0.isEmpty()) {
			List<ContextMenuItem> items = new ArrayList<>(List.of(
					new ContextMenuItem(playerRank.getDisplayName(), FaceIcon.getFace(new GameProfile(player.uuid, null)), () -> {}).setCloseMenu(false),
					ContextMenuItem.SEPARATOR
			));
			items.addAll(items0);
			openContextMenu(new ContextMenu(parent, items));
		}
	}
}
