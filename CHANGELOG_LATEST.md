The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

This is a copy of the changelog for the most recent version. For the full version history, go [here](https://github.com/pawjwp/diet/blob/1.20.x/CHANGELOG).

## [3.0.1+1.20.1] - 2026.05.07
This is a minor update to fix a few problems from the previous major update. See the [3.0 Changelog](https://github.com/pawjwp/diet/blob/1.20.x/CHANGELOG.md#3001201---20260505) for details about the new features.
### Added
- Drinks and other non-food edible items can now provide nutrition if present in the `food_values` config. Items must use the eating or drinking animation to be considered edible.
- Milk buckets now provide protein when data-driven food values are enabled in the config.
### Fixed
- Fixed some foods having the wrong `food_values`
    - Vanilla beetroot soup
    - Farmer's Delight tomato sauce
    - Farmer's Delight cabbage
- Fixed some typos in the default `food_values` that prevented some modded foods from registering nutrition properly
    - 10 food items from Corn Delight
    - 5 food items from Vegan Delight
    - 2 food items from Cultural Delights
