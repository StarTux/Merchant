# Merchant

The plugin allows the creation of custom merchant recipes in-game.
Merchants can be spawned in order to offer said trades to players.

## Storage

We keep track a list of recipes.  Aside from the items, each recipe
has a merchant name assigned.  Furthermore, we keep track of a list of
spawn locations, each of which has a merchant name.  Thus, the same
merchant can manifest in several spawns.

## Commands

### Recipes

- `/merchant create <name>` Create a recipe.
- `/merchant list` List recipes.
- `/merchant delete <num>` Delete recipe.
- `/merchant open <name> [player]` Open recipe for trade.

### Spawns

- `/merchant spawn <merchant>` Create a spawn.
- `/merchant listspawns` List all spawns.
- `/merchant deletespawn <num>` Delete a spawn.

### IO

- `/merchant reload` Reload recipes and spawns.
- `/merchant save` Save recipes and spawns.
