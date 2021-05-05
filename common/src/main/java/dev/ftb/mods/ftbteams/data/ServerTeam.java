package dev.ftb.mods.ftbteams.data;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.event.TeamEvent;
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
		save();
		manager.saveNow();
		manager.teamMap.remove(getId());
		String fn = getId() + ".snbt";

		try {
			Path dir = manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("deleted");

			if (Files.notExists(dir)) {
				Files.createDirectories(dir);
			}

			Files.move(manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("server/" + fn), dir.resolve(fn));
		} catch (IOException e) {
			e.printStackTrace();

			try {
				Files.deleteIfExists(manager.server.getWorldPath(TeamManager.FOLDER_NAME).resolve("server/" + fn));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		source.sendSuccess(new TextComponent("Team deleted"), true);
		manager.save();
		manager.syncAll();
		TeamEvent.DELETED.invoker().accept(new TeamEvent(this));
		return Command.SINGLE_SUCCESS;
	}
}
