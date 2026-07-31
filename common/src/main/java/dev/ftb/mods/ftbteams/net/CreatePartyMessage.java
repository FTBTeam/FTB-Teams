package dev.ftb.mods.ftbteams.net;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import dev.ftb.mods.ftbteams.FTBTeamsAPIImpl;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.PartyCreationValidator;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

public record CreatePartyMessage(String name, String description, int color, Set<GameProfile> invited) implements CustomPacketPayload {
	public static final Type<CreatePartyMessage> TYPE = new Type<>(FTBTeamsAPI.id("create_party"));

	public static final StreamCodec<FriendlyByteBuf, CreatePartyMessage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, CreatePartyMessage::name,
			ByteBufCodecs.STRING_UTF8, CreatePartyMessage::description,
			ByteBufCodecs.INT, CreatePartyMessage::color,
			ByteBufCodecs.GAME_PROFILE.apply(ByteBufCodecs.list()).map(Set::copyOf, List::copyOf), CreatePartyMessage::invited,
			CreatePartyMessage::new
	);

	public static void handle(CreatePartyMessage message, PacketContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		FTBTeamsAPI.api().getManager().getTeamForPlayer(player).ifPresent(team -> {
			var res = FTBTeamsAPIImpl.INSTANCE.validatePartyCreation(player);
			if (res.isSuccess()) {
				if (team instanceof PlayerTeam playerTeam) {
					try {
						playerTeam.createParty(player.getUUID(), player, message.name, message.description, message.color, message.invited);
					} catch (CommandSyntaxException e) {
						player.sendSystemMessage(Component.translatable("ftbteams.party_creation_failed", e.getMessage()));
					}
				}
			} else {
				player.sendSystemMessage(Component.translatable("ftbteams.party_api_only", res.reason().copy().withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.RED), false);
			}
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
