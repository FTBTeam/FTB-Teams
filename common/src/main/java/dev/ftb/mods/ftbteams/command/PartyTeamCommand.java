package dev.ftb.mods.ftbteams.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.FTBTeamsAPIImpl;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyArgument;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import dev.ftb.mods.ftbteams.data.TeamType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;

import static dev.ftb.mods.ftbteams.command.FTBTeamsCommands.*;

public class PartyTeamCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("party")
                .then(Commands.literal("create")
                        .requires(FTBTeamsCommands::hasNoParty)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> tryCreateParty(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                        .executes(ctx -> tryCreateParty(ctx.getSource(), ""))
                )
                .then(Commands.literal("join")
                        .requires(FTBTeamsCommands::hasNoParty)
                        .then(createTeamArg(TeamType.PARTY)
                                .executes(ctx -> partyTeamArg(ctx, TeamRank.INVITED).join(ctx.getSource().getPlayerOrException()))
                        )
                )
                .then(Commands.literal("decline")
                        .requires(FTBTeamsCommands::hasNoParty)
                        .then(createTeamArg(TeamType.PARTY)
                                .executes(ctx -> partyTeamArg(ctx, TeamRank.INVITED).declineInvitation(ctx.getSource()))
                        )
                )
                .then(Commands.literal("leave")
                        .requires(source -> hasParty(source, TeamRank.MEMBER))
                        .executes(ctx -> getPartyTeam(ctx, TeamRank.MEMBER).leave(ctx.getSource().getPlayerOrException().getUUID()))
                )
                .then(Commands.literal("invite")
                        .requires(source -> hasParty(source, TeamRank.OFFICER))
                        .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                .executes(ctx -> getPartyTeam(ctx, TeamRank.OFFICER).invite(ctx.getSource().getPlayerOrException(), GameProfileArgument.getGameProfiles(ctx, "players")))
                        )
                )
                .then(Commands.literal("kick")
                        .requires(source -> hasParty(source, TeamRank.OFFICER))
                        .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                .executes(ctx -> getPartyTeam(ctx, TeamRank.OFFICER).kick(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "players")))
                        )
                )
                .then(Commands.literal("transfer_ownership")
                        .requires(source -> hasParty(source, TeamRank.OWNER))
                        .then(Commands.argument("player_id", GameProfileArgument.gameProfile())
                                .executes(ctx -> getPartyTeam(ctx, TeamRank.OWNER).transferOwnership(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player_id")))
                        )
                )
                .then(Commands.literal("transfer_ownership_for")
                        .requires(requiresOPorSP())
                        .then(createTeamArg(TeamType.PARTY)
                                .then(Commands.argument("player_id", GameProfileArgument.gameProfile())
                                        .executes(ctx -> partyTeamArg(ctx, TeamRank.NONE).transferOwnership(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player_id")))
                                )
                        )
                )
                .then(Commands.literal("settings")
                        .requires(source -> hasParty(source, TeamRank.OWNER))
                        .then(Commands.argument("key", TeamPropertyArgument.create())
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .executes(ctx -> getPartyTeam(ctx, TeamRank.OWNER).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), string(ctx, "value")))
                                )
                                .executes(ctx -> getPartyTeam(ctx, TeamRank.OWNER).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), ""))
                        )
                )
                .then(Commands.literal("settings_for")
                        .requires(requiresOPorSP())
                        .then(createTeamArg(TeamType.PARTY)
                                .then(Commands.argument("key", TeamPropertyArgument.create())
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> partyTeamArg(ctx, TeamRank.NONE).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), string(ctx, "value")))
                                        )
                                        .executes(ctx -> partyTeamArg(ctx, TeamRank.NONE).settings(ctx.getSource(), TeamPropertyArgument.get(ctx, "key"), ""))
                                )
                        )
                )
                .then(Commands.literal("allies")
                        .requires(source -> hasParty(source, TeamRank.MEMBER))
                        .then(Commands.literal("add")
                                .requires(source -> hasParty(source, TeamRank.OFFICER))
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(ctx -> getPartyTeam(ctx, TeamRank.OFFICER).addAlly(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player")))
                                )
                        )
                        .then(Commands.literal("remove")
                                .requires(source -> hasParty(source, TeamRank.OFFICER))
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(ctx -> getPartyTeam(ctx, TeamRank.OFFICER).removeAlly(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player")))
                                )
                        )
                        .then(Commands.literal("list")
                                .requires(source -> hasParty(source, TeamRank.MEMBER))
                                .executes(ctx -> getPartyTeam(ctx, TeamRank.MEMBER).listAllies(ctx.getSource()))
                        )
                );
    }

    private static int tryCreateParty(CommandSourceStack source, String partyName) throws CommandSyntaxException {
        if (TeamManagerImpl.INSTANCE != null) {
            if (FTBTeamsAPIImpl.INSTANCE.isPartyCreationFromAPIOnly()) {
                throw TeamArgument.API_OVERRIDE.create();
            }
            TeamManagerImpl.INSTANCE.createParty(source.getPlayerOrException(), partyName);
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
}
