package com.cavetale.merchant;

import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.util.Json;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;

@RequiredArgsConstructor
final class MerchantCommand implements TabExecutor {
    private final MerchantPlugin plugin;
    private CommandNode rootNode;

    protected void enable() {
        rootNode = new CommandNode("merchant");
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload configuration")
            .senderCaller(this::reload);
        rootNode.addChild("save").denyTabCompletion()
            .description("Save to disk")
            .senderCaller(this::save);
        // recipe
        CommandNode recipeNode = rootNode.addChild("recipe").description("Recipes");
        recipeNode.addChild("create").arguments("<name> [maxUses]")
            .description("Create merchant")
            .playerCaller(this::recipeCreate);
        recipeNode.addChild("list").denyTabCompletion()
            .description("List merchants")
            .senderCaller(this::recipeList);
        recipeNode.addChild("edit").arguments("<num> [maxUses]")
            .description("Edit merchant")
            .playerCaller(this::recipeEdit);
        recipeNode.addChild("delete").arguments("<num>")
            .description("Delete merchant")
            .senderCaller(this::recipeDelete);
        recipeNode.addChild("open").arguments("<name> [player]")
            .description("Open recipe")
            .senderCaller(this::recipeOpen);
        // spawn
        CommandNode spawnNode = rootNode.addChild("spawn").description("Spawns");
        spawnNode.addChild("create").arguments("<merchant>")
            .description("Create spawn")
            .playerCaller(this::spawnCreate);
        spawnNode.addChild("list").denyTabCompletion()
            .description("List spawns")
            .senderCaller(this::spawnList);
        spawnNode.addChild("delete").arguments("<num>")
            .description("Delete spawn")
            .senderCaller(this::spawnDelete);
        spawnNode.addChild("displayname").arguments("<index> <json...>")
            .description("Set display name")
            .senderCaller(this::spawnDisplayName);
        spawnNode.addChild("setvillager").arguments("<index> <profession> <type> <level>")
            .description("Set villager type")
            .completers(CommandArgCompleter.integer(i -> i > 0),
                        CommandArgCompleter.enumLowerList(Villager.Profession.class),
                        CommandArgCompleter.enumLowerList(Villager.Type.class),
                        CommandArgCompleter.integer(i -> i >= 1 && i <= 5))
            .senderCaller(this::spawnSetVillager);
        // finis
        plugin.getCommand("merchant").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        return rootNode.call(sender, command, alias, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return rootNode.complete(sender, command, alias, args);
    }

    boolean recipeCreate(Player player, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        RecipeMakerMenu menu = new RecipeMakerMenu(args[0]);
        int maxUses = -1;
        if (args.length >= 2) {
            try {
                maxUses = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                throw new CommandWarn("Invalid maxUses: " + args[1]);
            }
        }
        Inventory inventory = plugin.getServer()
            .createInventory(menu, 9, Component.text("New Merchant Recipe"));
        menu.inventory = inventory;
        menu.maxUses = maxUses;
        player.openInventory(inventory);
        return true;
    }

    boolean recipeList(CommandSender sender, String[] args) {
        int i = 0;
        for (Recipe recipe : plugin.merchants.recipes.recipes) {
            sender.sendMessage(Component.text("" + (i++) + ") "
                                              + recipe.merchant + ": "
                                              + Items.toString(recipe)
                                              + " max=" + recipe.maxUses,
                                              NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text(i + " total recipes", NamedTextColor.YELLOW));
        return true;
    }

    boolean recipeDelete(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        int i = intOf(args[0]);
        if (i < 0 || i >= plugin.merchants.recipes.recipes.size()) {
            throw new CommandWarn("Illegal index: " + i);
        }
        Recipe recipe = plugin.merchants.recipes.recipes.remove(i);
        plugin.merchants.save();
        sender.sendMessage(Component.text("Deleted recipe #" + i + ": " + Items.toString(recipe), NamedTextColor.YELLOW));
        return true;
    }

    boolean recipeEdit(Player player, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        int i = intOf(args[0]);
        if (i < 0 || i >= plugin.merchants.recipes.recipes.size()) {
            throw new CommandWarn("Illegal index: " + i);
        }
        int maxUses = -1;
        if (args.length >= 2) {
            try {
                maxUses = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                throw new CommandWarn("Invalid maxUses: " + args[1]);
            }
        }
        Recipe recipe = plugin.merchants.recipes.recipes.get(i);
        RecipeMakerMenu menu = new RecipeMakerMenu(recipe.merchant);
        menu.recipe = recipe;
        Inventory inventory = plugin.getServer()
            .createInventory(menu, 9, Component.text("Edit Merchant Recipe"));
        inventory.setItem(0, Items.deserialize(recipe.inA));
        inventory.setItem(1, Items.deserialize(recipe.inB));
        inventory.setItem(2, Items.deserialize(recipe.out));
        menu.inventory = inventory;
        menu.maxUses = maxUses;
        player.openInventory(inventory);
        return true;
    }

    boolean recipeOpen(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length != 2) return false;
        Player target;
        if (args.length >= 2) {
            String name = args[1];
            target = plugin.getServer().getPlayer(name);
            if (target == null) throw new CommandWarn("Player not found: " + name);
        } else {
            target = playerOf(sender);
        }
        String merchant = args[0];
        plugin.merchants.openMerchant(target, merchant, Component.text(merchant));
        sender.sendMessage(Component.text(target.getName() + " opened merchant " + merchant, NamedTextColor.YELLOW));
        return true;
    }

    boolean spawnCreate(Player player, String[] args) {
        if (args.length != 1) return false;
        String merchant = args[0];
        Spawn spawn = new Spawn();
        spawn.load(player.getLocation());
        spawn.merchant = merchant;
        plugin.merchants.recipes.spawns.add(spawn);
        plugin.merchants.save();
        plugin.merchants.spawnAll();
        player.sendMessage(Component.text(merchant + " spawn created at current location", NamedTextColor.YELLOW));
        return true;
    }

    boolean spawnList(CommandSender sender, String[] args) {
        int i = 0;
        for (Spawn spawn : plugin.merchants.recipes.spawns) {
            sender.sendMessage(Component.text("" + (i++) + ") "
                                              + spawn.simplified(),
                                              NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text(i + " total spawns", NamedTextColor.YELLOW));
        return true;
    }

    boolean spawnDelete(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        int i = intOf(args[0]);
        if (i < 0 || i >= plugin.merchants.recipes.spawns.size()) {
            throw new CommandWarn("Illegal index: " + i);
        }
        Spawn spawn = plugin.merchants.recipes.spawns.remove(i);
        plugin.merchants.save();
        plugin.merchants.clearMobs();
        plugin.merchants.spawnAll();
        sender.sendMessage(Component.text("Deleted spawn #" + i + ": " + spawn.simplified(), NamedTextColor.YELLOW));
        return true;
    }

    boolean reload(CommandSender sender, String[] args) {
        plugin.merchants.load();
        sender.sendMessage(Component.text("Recipes reloaded", NamedTextColor.YELLOW));
        return true;
    }

    boolean save(CommandSender sender, String[] args) {
        plugin.merchants.save();
        sender.sendMessage(Component.text("Recipes saved", NamedTextColor.YELLOW));
        return true;
    }

    protected boolean spawnDisplayName(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Spawn spawn = spawnOf(args[0]);
        String json = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Component displayName = Json.deserializeComponent(json);
        if (displayName == null || Component.empty().equals(displayName)) {
            throw new CommandWarn("Invalid JSON. See console!");
        }
        spawn.setDisplayName(displayName);
        plugin.merchants.save();
        plugin.merchants.clearMobs();
        plugin.merchants.spawnAll();
        sender.sendMessage(Component.text().content("Display name set to ").color(NamedTextColor.YELLOW)
                           .append(spawn.getDisplayName()));
        return true;
    }

    protected boolean spawnSetVillager(CommandSender sender, String[] args) {
        if (args.length != 4) return false;
        Spawn spawn = spawnOf(args[0]);
        Spawn.Appearance appearance = new Spawn.Appearance();
        appearance.entityType = EntityType.VILLAGER;
        try {
            appearance.villagerProfession = Villager.Profession.valueOf(args[1].toUpperCase());
            appearance.villagerType = Villager.Type.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Invalid profession or type: " + args[1] + ", " + args[2]);
        }
        appearance.villagerLevel = intOf(args[3]);
        if (appearance.villagerLevel < 1 || appearance.villagerLevel > 5) {
            throw new CommandWarn("Illegal villager level: " + appearance.villagerLevel);
        }
        spawn.appearance = appearance;
        plugin.merchants.save();
        plugin.merchants.clearMobs();
        plugin.merchants.spawnAll();
        sender.sendMessage(Component.text("Villager appearance updated", NamedTextColor.YELLOW));
        return true;
    }

    private List<String> complete(final String arg, final Stream<String> opt) {
        return opt.filter(o -> o.startsWith(arg))
            .collect(Collectors.toList());
    }

    Player playerOf(final CommandSender sender) throws CommandWarn {
        if (!(sender instanceof Player)) {
            throw new CommandWarn("Player expected");
        }
        return (Player) sender;
    }

    private int intOf(final String arg) throws CommandWarn {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            throw new CommandWarn("Not a number: " + arg);
        }
    }

    private Spawn spawnOf(final String arg) throws CommandWarn {
        int index = intOf(arg);
        if (index < 0 || index >= plugin.merchants.recipes.spawns.size()) {
            throw new CommandWarn("Illegal spawn index: " + index);
        }
        return plugin.merchants.recipes.spawns.get(index);
    }
}
