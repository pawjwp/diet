The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

This is a copy of the changelog for the most recent version. For the full version history, go [here](https://github.com/pawjwp/diet/blob/1.20.x/CHANGELOG.md).

## [3.1.0+1.20.1] - 2026.10.02
This update completes some partially implemented features related to diet suite switching, allows diet gain at full hunger, and fixes multiplayer syncing with custom food values enabled.
### Added
- Diet suites can now be switched in-game
    - `/diet suite <player> <suite>` switches the player to another suite
    - `/diet suite <player>` shows a player's current suite
    - Food groups in both suites keep their values, while any new groups start at their `default_value`
    - A player's suite is now saved with the rest of their diet data and persists through death and reloads
    - If a suite cannot be loaded, players are moved back to the default suite
    - The `DietApi` methods `getSuite`, `setSuite`, and `getSuites` are now functional, so other mods can read and change a player's suite
    - Tooltips and eaten food only apply to groups in the selected suite
- New server config options:
    - `defaultSuite` lets the default suite be changed to something other than `builtin` by default
    - `requireHungerGain` restores the previous behavior where food only increases diet values if it also increases the player's hunger
- Added Spanish translation, thanks to [elbuda](https://github.com/elbuda)
### Changed
- Foods can now increase diet values even if they don't increase hunger (meaning foods like golden apples and chorus fruit can continuously increase diet values)
- The default diet suite now gives Hunger III instead of Hunger IV at high sugars
### Fixed
- Fixed food value syncing on multiplayer servers, tooltips on the client should now show the right food values.
- Fixed `food_values` files loading in an arbitrary order, they now load like vanilla tags
  - First, the version of a file from all datapacks is combined in the datapacks' order
  - `replace` will now discard only lower priority versions of the same file
  - Values from higher priority datapacks override lower ones
  - Errored files are now skipped earlier to avoid possibly overriding other files
- Fixed some foods having the wrong `food_values`
    - Farmer's Delight cabbage
    - Farmer's Delight minced beef
- Fixed the wrong error message appearing for an invalid status effect