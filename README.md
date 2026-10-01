# A Balanced Diet

## About

A Balanced Diet is a fork of the [Diet](https://github.com/illusivesoulworks/diet) mod, which facilitates the creation and management of dietary food groups in Minecraft. Diet comes with a default configuration that creates five classical food groups (fruits, grains, vegetables, proteins, and sugars). The mod is highly configurable; users and modpack developers can define their own food groups, classifications, diet effects, notifications, etc.

This fork was created to add some additional clarity, balance, and quality of life features. These would have been made as a PR to the original mod, but the author stated the mod is not likely to have any further 1.20.1 updates. These new features are almost entirely optional and data-driven, and include a [notification system](https://github.com/pawjwp/diet#notifications) for crossing specified thresholds, [quality overlays](https://github.com/pawjwp/diet#quality-view) to the diet bars that show what thresholds start providing positive or negative effects, and [per-food nutrition](https://github.com/pawjwp/diet#per-food-nutrition) definition.

## Downloads

[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/project/a-balanced-diet) [![Available on Curseforge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/a-balanced-diet)

## Features

### Food Groups

![](https://i.ibb.co/BLYDcbT/diet-screen.png)

Food groups are custom dietary groups that represent the types of food that you have eaten. Each group has a value ranging between 0% and 100% depending on how much of that particular category that a player has eaten. These values increase depending on what types of food a player eats and every group gradually decays when the player uses up their hunger bar.

By default, Diet comes with five classical food groups: Fruits, Grains, Proteins, Vegetables, and Sugars. Nutrition values are typically determined by tags. However, if enabled in the config, they can be [set per-food](https://github.com/pawjwp/diet#per-food-nutrition) instead.

By creating data files, users and modpack developers can create their own custom food groups. Data files can be loaded as part of a datapack or using a mod like [KubeJS](https://modrinth.com/mod/kubejs) or [Open Loader](https://modrinth.com/mod/open-loader). Configurable options include:
- Name
- Item Icon
- Hexcode Color
- Ordering
- Default Value
- Gain Multiplier
- Decay Multiplier
- Beneficial or not

### Dietary Effects

![](https://i.ibb.co/7vmZfpD/diet-effects.png)

Dietary effects are custom rewards or penalties applied to players based on certain, configurable food group values. These effects are defined in diet suites, which are created through data files in the same way as food groups. A diet suite lists the food groups it uses along with the effects that come from them.

Possible effects can include any registered potion effect, vanilla and modded, as well as modifying attributes directly (i.e. increasing maximum health by an arbitrary value). The conditions for these effects are highly configurable, including checking specific values, checking only subsets of groups, applying effects cumulatively for each matching test, and much more.

### Quality View

The quality view shows the "quality" of a food group, which indicates to players what thresholds will provide positive or negative effects. Using the `qualityDisplayMode` option in the client config, it can be set to appear upon hovering over a group bar, by clicking a button in the corner of the Diet screen, both, or neither (the default).

Quality values can be defined for any effect in a diet suite using the `quality` field, which is set to a string containing a hex color code (ex: `"#3FDF3F"`). This color is drawn over that region of the bar whenever the quality view is visible.

### Per-Food Nutrition

If the `enableDataFoodValues` option is enabled in the server config, nutrition values can be set per-food rather than generated based on tags. Diet will reference data files at `diet/food_values` to get food values on a per-food basis, which are used in place of the tag-based ones.

The default datapack has values configured for ~20 mods. The list of compatible mods in these data files can be viewed [here](https://github.com/pawjwp/diet/tree/1.20.x/common/src/main/resources/data/diet/diet/food_values).

### Notifications

Notifications are an optional system that alerts players in chat when their food group values cross a threshold. They are customized from within a diet suite by adding a `notification` object to a diet effect. Configurable options include:
- Notification ID
- Message (uses translation keys)
- Notification Sets (used for bulk-muting)
- Trigger (controls what causes the notification to appear)
- Default Frequency (how many times the notification will appear)
  - `always`, notification will appear every time its trigger is met
  - `once`, notification will appear once and then set itself to never
  - `never`, notification will not appear unless triggered by testing commands

If a notification's frequency is set to `always`, it will provide a prompt for the player to mute it. When clicked, the mute menu opens in chat with options to mute this notification, all notifications in the same set, all notifications for the same food group, or all diet notifications. Players can also adjust their notification settings manually using the `/diet notifications` commands and the entire notification system can be disabled with the `notificationsEnabled` option in the server config.

### Further Reading

Please refer to the original mod's [wiki](https://docs.illusivesoulworks.com/1.20.x/category/diet) for more detailed information about food groups, diet suites, and effects. Features added in 3.0+ are described above in [Quality View](https://github.com/pawjwp/diet#quality-view), [Per-Food Nutrition](https://github.com/pawjwp/diet#per-food-nutrition), and [Notifications](https://github.com/pawjwp/diet#notifications). Examples of most mod features in-action can be found [here](https://github.com/pawjwp/Desolate-Planet/tree/main/kubejs/data/desolate_planet/diet), in the data files of my modpack, Desolate Planet.

### Commands

Diet registers a few commands to help aid debugging and server management.

- `/diet`
    - `get <player> <group>`
    - `set <player> <group> <value>`
    - `add <player> <group> <value>`
    - `subtract <player> <group> <value>`
    - `reset <player>`
    - `suite <player> [<suite>]`
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

If preferred, I am active in the [Discord](https://discord.gg/4en3SpWtJg) server for my current modpack, [Desolate Planet](https://modrinth.com/modpack/desolate-planet/). Feel free to ping me there and I should respond when able.

## License

All source code and assets are licensed under LGPL 3.0.

## Donations

Donations to the original mod's author can be sent through [Ko-fi](https://ko-fi.com/C0C1NL4O).

All proceeds from the ad revenue on this project will be donated to [GiveWell](https://www.givewell.org/).

## Compatibility

The standard "five food groups" tag system supports many mods, which can be viewed [here](https://github.com/illusivesoulworks/diet#five-food-groups---supported-mods), in the original mod's description.

A smaller subset of mods are supported by the custom food values system. A list of these mods can be found in the mod's [default datapack](https://github.com/pawjwp/diet/tree/1.20.x/common/src/main/resources/data/diet/diet/food_values). These default values assume a decreased drain rate for fruits and vegetables, and greatly increased drain rates for sugars.

If you would like to request additional support for this system, please open an [issue](https://github.com/pawjwp/diet/issues) or open a [pull request](https://github.com/pawjwp/diet/pulls) to contribute directly.
