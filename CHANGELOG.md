# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2101.1.8]

### Added
* Expanded the team properties system somewhat
  * New team property types: Big Integer, String set, and String to int/bool/string map
  * Added API to make it easier to register more custom property types if needed
  * Added new builtin "Team Stage" team property, a team-based game stages system

## [2101.1.7]

### Added
* Added API method to create server teams with fixed UUIDs

## [2101.1.6]

### Added
* Added `/ftbteams nbtedit` admin command to allow direct NBT viewing/editing of team data
  * FTB Library 2021.1.25+ required
  * Use with caution; incorrect edits could leave the team in an unusable state. Make backups!

### Fixed
* Team "extra data" is now copied from player to party team when the party team is initially formed.

## [2101.1.5]

### Added

* Added `/ftbteams force-add` and `/ftbteams force-remove` commands
  * Allows an admin user to quickly add/remove any player (including offline players if known to server) to/from any team

### Fixed
* Fixed "internal error" message when using `/ftbteams transfer_ownership` command
* Fixed output formatting for error message when trying to transfer ownership to an invalid player
* Fixed a server crash if vanilla clients attempt to connect, and no other installed mod has already prevented a connection
  * This technically means FTB Teams can now be used as a (limited) server-only mod, without any GUI functionality or translations
  * Lack of translations could be fixed by making a resource pack containing the files in https://github.com/FTBTeam/FTB-Teams/blob/dev/common/src/main/resources/assets/ftbteams/lang/

## [2101.1.4]

### Fixed
* Fixed a FTB Team Bases related crash when attempting to visit bases of disbanded teams

## [2101.1.3]

### Added 
* Added per-player toggle to redirect chat into ftb teams chat
  * When enabled, all messages typed into chat will go only to the player's team instead of the usual broadcast 
  * Use `/ftbteams redirect_chat` command to toggle
  * Or use the new toggle button on the teams GUI (chat icon, top right)
* Updated `es_es` and `es_mx` translations (thanks @ArrivedBog593)
* Updated `fr_fr` translation (thanks @nogapra)
* Added `ja_jp` translation (thanks @Twister)
* Added `pt_br` translation (thanks @Xlr11)
* Added `uk_ua` translation (thanks @GIGABAIT93)
* Added `ru_ru` translation (thanks @BazZziliuS)

## [2101.1.2]

### Changed
* FTB Teams commands which provide command completion for known teams are now smarter about suggesting only relevant teams 
  * e.g. `/ftbteams server settings ...` now only lists known server teams, not other team types

### Fixed
* Fixed bug where renaming a team's display name didn't sync change to clients immediately, causing command completion to provide the previous name
  * This also means any team names and colors will now be immediately visible on client display (e.g. FTB Chunks) if changed by command
 
## [2101.1.1]

### Added
* Added a `tr_tr` translation, thanks @RuyaSavascisi

### Changed
* Overhauled and cleaned up many icon textures

### Fixed
* Fixed the `/ftbteams party settings_for ...` command not functioning correctly

## [2101.1.0]

### Changed
* Ported to MC 1.21.1
  * Will not run on MC 1.21

### Added
* Sidebar buttons for this and other FTB mods can now be enabled/disabled/rearranged (new functionality in FTB Library 2101.1.0)

## [2100.1.0]

### Changed
* Ported to MC 1.21

## [2006.1.0]

### Changed
* Ported to Minecraft 1.20.6. Support for Fabric and NeoForge.
  * Forge support may be re-added if/when Architectury adds support for Forge

## [2004.1.2]

### Changed
* Ported to Minecraft 1.20.4. Support for Fabric, Forge and NeoForge.

## [2001.2.0]

### Changed
* Technical addition: property defaults are now initialised lazily, via supplier
  * No player-visible change to this right now, but it makes it easier for mods to define config-based defaults, e.g. FTB Chunks team properties
  * Technically there is an API break, but only if you were trying to construct property objects yourself using the from-network constructor (and there was no good reason to do that!)
* Deprecated the `CustomPartyCreationHandler` object and related API methods
  * Replaced with simpler `FTBTeamsAPI#setPartyCreationFromAPIOnly(boolean)` method
  * Intended for use in custom modpacks where players shouldn't create teams directly themselves (either via CLI or GUI)
* Updated party team creation API to work without requiring the party-owning player to be online

### Added
* Added fr_fr translation (thanks @HollishKid and @K0LALA)

### Fixed
* Fixed the `/ftbteams party info` command always showing your own party info, regardless of team that was passed to the command

## [2001.1.4]

### Fixed
* Fixed team property changes not getting saved correctly

## [2001.1.3]

### Fixed
* Fixed `/ftbteams party join` and `/ftbteams party decline` commands not working correctly

## [2001.1.2]

### Added
* Ported to Minecraft 1.20.1
  * Equivalent in functionality to the 1902.2.14 release

## [1902.2.12]

### Added
* Team properties can now include properties which are lists of string (required by new FTB Chunks builds)

### Fixed
* Major improvements in efficiency of server->client sync for team data
  * Should greatly reduce network traffic and load for busy servers (many players & teams) in particular
* A few GUI and logic fixes related to handling invites for team members and allies
  * Allow players to be added as allies of your team even when they are a member of a different team
  * Don't allow invitations to be sent to players who are already in a different team (they couldn't actually be added, but a useless invitation was being sent)
  * Only show the GUI "Manage Allies" and "Invite Players" buttons for party teams
  * Don't show "Disband Party" context menu entry in the GUI for non-party teams
* Don't allow server teams to be created with names shorter than 3 characters
* Converted a couple of more messages into translations

## [1902.2.11]

### Fixes
* Fixed client-side NPE's when teams data is unavailable on the client
  * Doesn't fix the root cause, which is that for some reason client has not received valid teams data from the server
  * This could occur if trying to play in offline mode, which is not supported

## [1902.2.10]

### Added
* Major GUI overhaul; it is now possible to do just about anything with the GUI that can be done with the `/ftbteams` command
  * Players in the teams GUI can now be clicked for a context menu with applicable operations, based on your rank and their
  * If you are officer or owner, buttons are visible at the top to invite players to your party, or manage team allies
  * Got rid of the "WIP" message :)
* The team chat history now has a maximum size, default 1000 lines
  * This can be adjusted up or down via the team property settings
* Team properties are now separated into categories in the settings GUI, based on which mod registered the properties
  * E.g. FTB Chunks properties are in their own subsection, separate from basic team properties
* API: new `TeamAllyEvent` is fired when an ally is added or removed
* Pressing Tab in the teams GUI gives focus to the chat input textbox
* Converted many messages into translations
