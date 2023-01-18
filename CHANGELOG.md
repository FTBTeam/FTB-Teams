# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1902.2.12]

### Added
* Team properties can now include properties which are lists of string (required by new FTB Chunks builds)

### Fixed
* A few GUI and logic fixes related to handling invites for team members and allies
  * Allow players to be added as allies of your team even when they are a member of a different team
  * Don't allow invitations to be sent to players who are already in a different team (they couldn't actually be added, but a useless invitation was being sent)
  * Only show the GUI "Manage Allies" and "Invite Players" buttons for party teams

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
