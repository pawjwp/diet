# A Balanced Diet

## About

A Balanced Diet is a fork of the [Diet](https://github.com/illusivesoulworks/diet), which facilitates the creation and management of dietary food groups in Minecraft. Diet comes with a default configuration that creates five classical food groups (fruits, grains, vegetables, proteins, and sugars). The mod is highly configurable; users and modpack developers can define their own food groups, classifications, diet effects, notifications, etc.

This fork was created to add some additional clarity, balance, and quality of life features. These would have been made as a PR to the original mod, but the author stated the mod is not likely to have any further 1.20.1 updates. These new features are almost entirely optional and data-driven, and include a notification system for crossing specified thresholds, quality overlays to the diet bars that show what thresholds start providing positive or negative effects, and per-food nutrition definition. For more information about these new features see the [3.0 Changelog](https://github.com/pawjwp/diet/blob/1.20.x/CHANGELOG.md#3001201---20260505).

## Downloads

[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/project/a-balanced-diet) [![Available on Curseforge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/a-balanced-diet)

## Features

### Food Groups

![](https://i.ibb.co/BLYDcbT/diet-screen.png)

Food groups are custom dietary groups that represent the types of food that you have eaten. Each group has a value ranging between 0% and 100% depending on how much of that particular category that a player has eaten. These values increase depending on what types of food a player eats and every group gradually decays when the player uses up their hunger bar.

By default, Diet comes with five classical food groups: Fruits, Grains, Proteins, Vegetables, and Sugars. Nutrition values are typically determined by tags. However, if enabled in the config, they can be set per-food based on datapack files at `/diet/food_values`.

By editing the `diet-groups.toml` configuration file in the world save's `serverconfig` folder, users and modpack developers can create their own custom food groups. Configurable options include:
- Name
- Item Icon
- Hexcode Color
- Ordering
- Default Value
- Gain Multiplier
- Decay Multiplier

Please refer to the [wiki](https://github.com/TheIllusiveC4/Diet/wiki) for more detailed information. For information about 3.0+ features (like quality indicators, per-food nutrition, and notifications), see the [3.0 Changelog](https://github.com/pawjwp/diet/blob/1.20.x/CHANGELOG.md#3001201---20260505).

### Dietary Effects

![](https://i.ibb.co/7vmZfpD/diet-effects.png)

Dietary effects are custom rewards or penalties applied to players based on certain, configurable food group values. These effects can be configured through the `diet-effects.toml` file in the world save's `serverconfig` folder.

Possible effects can include any registered potion effect, vanilla and modded, as well as modifying attributes directly (i.e. increasing maximum health by an arbitrary value). The conditions for these effects are highly configurable, including checking specific values, checking only subsets of groups, applying effects cumulatively for each matching test, and much more.

Please refer to the [wiki](https://github.com/TheIllusiveC4/Diet/wiki) for more detailed information. For information about 3.0+ features (like quality indicators, per-food nutrition, and notifications), see the [3.0 Changelog](https://github.com/pawjwp/diet/blob/1.20.x/CHANGELOG.md#3001201---20260505).

### Commands

Diet registers a few commands to help aid debugging and server management.

- `/diet`
    - `get <player> <group>`
    - `set <player> <group> <value>`
    - `add <player> <group> <value>`
    - `subtract <player> <group> <value>`
    - `reset <player>`
    - `pause <player>`
    - `resume <player>`
    - `export <filter> <argument>`
    - `notify <target> <notification_id>`
    - `notifications list`
    - `notifications all`
    - `notifications group <group_id>`
    - `notifications set <set_id>`
    - `notifications message <message_id>`
    - `notifications options <message_id>`
    - `notifications reset`

## Support

Please report all bugs, issues, and feature requests using the issue tracker on the mod's [GitHub](https://github.com/pawjwp/diet/issues) page. Please do not make issues on the original mod's GitHub page for features only present in this fork.

If preferred, I will respond to any pings in the [Discord](https://discord.gg/4en3SpWtJg) server for my current modpack, [Desolate Planet](https://modrinth.com/modpack/desolate-planet/). Feel free to reach out to me there as well.

## License

All source code and assets are licensed under LGPL 3.0.

## Donations

Donations to the original mod's author can be sent through [Ko-fi](https://ko-fi.com/C0C1NL4O).

All proceeds from the ad revenue on this project will be donated to [GiveWell](https://www.givewell.org/).

## Compatibility

The standard "five food groups" tag system supports many mods, which can be viewed [here](https://github.com/illusivesoulworks/diet#five-food-groups---supported-mods), in the original mod's description.

A smaller subset of mods are supported by the custom food values system. A list of these mods can be found in the mod's [default datapack](https://github.com/pawjwp/diet/tree/1.20.x/common/src/main/resources/data/diet/diet/food_values). These default values assume a decreased drain rate for fruits and vegetables, and greatly increased drain rates for sugars.

If you would like to request additional support for this system, please open an [issue](https://github.com/pawjwp/diet/issues) or open a [pull request](https://github.com/pawjwp/diet/pulls) to contribute directly.
