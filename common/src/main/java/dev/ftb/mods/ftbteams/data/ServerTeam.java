package dev.ftb.mods.ftbteams.data;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbteams.event.TeamDeletedEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerTeam extends Team {
	public ServerTeam(TeamManager m) {
		super(m);
	}

	@Override
	public TeamType getType() {
		return TeamType.SERVER;
	}

	@Deprecated
	public int delete(CommandSourceStack source) throws CommandSyntaxException {
		TeamDeletedEvent.EVENT.invoker().accept(new TeamDeletedEvent(this));
		save();
		manager.saveNow();
		manager.teamMap.remove(id);

		try {
			Path dir = manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("deleted");

			if (Files.notExists(dir)) {
				Files.createDirectories(dir);
			}

			String fn = UUIDTypeAdapter.fromUUID(id) + ".nbt";
			Files.move(manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("server/" + fn), dir.resolve(fn));
		} catch (IOException e) {
			e.printStackTrace();
		}

		source.sendSuccess(new TextComponent("Team deleted"), true);
		manager.save();
		manager.syncAll();
		return Command.SINGLE_SUCCESS;
	}
}
