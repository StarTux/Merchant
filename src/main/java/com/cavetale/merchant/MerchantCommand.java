package com.cavetale.merchant;

import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.util.Json;
import com.cavetale.merchant.save.MerchantFile;
import com.cavetale.merchant.save.Recipe;
import com.cavetale.merchant.save.Spawn;
import java.util.ArrayList;
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
        // recipe
        CommandArgCompleter merchantFileNameCompleter = CommandArgCompleter
            .supplyList(() -> new ArrayList<>(plugin.merchants.merchantFileMap.keySet()));
        CommandNode recipeNode = rootNode.addChild("recipe").description("Recipes");
        recipeNode.addChild("create").arguments("<merchant> [maxUses]")
            .completers(merchantFileNameCompleter,
                        CommandArgCompleter.integer(i -> i >= -1))
            .description("Create merchant")
            .playerCaller(this::recipeCreate);
        recipeNode.addChild("list").arguments("[merchant]")
            .completers(merchantFileNameCompleter)
            .description("List merchants")
            .senderCaller(this::recipeList);
        recipeNode.addChild("edit").arguments("<merchant> <num> [maxUses]")
            .completers(merchantFileNameCompleter,
                        CommandArgCompleter.integer(i -> i >= 0),
                        CommandArgCompleter.integer(i -> i >= -1))
            .description("Edit merchant")
            .playerCaller(this::recipeEdit);
        recipeNode.addChild("delete").arguments("<merchant> <num>")
            .completers(merchantFileNameCompleter,
                        CommandArgCompleter.integer(i -> i >= 0))
            .description("Delete recipe")
            .senderCaller(this::recipeDelete);
        recipeNode.addChild("open").arguments("<merchant> [player]")
            .completers(merchantFileNameCompleter,
                        CommandArgCompleter.NULL)
            .description("Open recipe")
            .senderCaller(this::recipeOpen);
        recipeNode.addChild("move").arguments("<merchant> <from> <to>")
            .completers(merchantFileNameCompleter,
                        CommandArgCompleter.integer(i -> i > 0),
                        CommandArgCompleter.integer(i -> i > 0))
            .description("Move recipe to new index")
            .senderCaller(this::recipeMove);
        // spawn
        CommandArgCompleter spawnNameCompleter = CommandArgCompleter
            .supplyList(() -> new ArrayList<>(plugin.merchants.spawnMap.keySet()));
        CommandNode spawnNode = rootNode.addChild("spawn").description("Spawns");
        spawnNode.addChild("create").arguments("<merchant> [name]")
            .completers(merchantFileNameCompleter,
                        CommandArgCompleter.EMPTY)
            .description("Create spawn")
            .playerCaller(this::spawnCreate);
        spawnNode.addChild("list").denyTabCompletion()
            .description("List spawns")
            .senderCaller(this::spawnList);
        spawnNode.addChild("delete").arguments("<name>")
            .completers(spawnNameCompleter)
            .description("Delete spawn")
            .senderCaller(this::spawnDelete);
        spawnNode.addChild("displayname").arguments("<name> <json...>")
            .completers(spawnNameCompleter)
            .description("Set display name")
            .senderCaller(this::spawnDisplayName);
        spawnNode.addChild("setvillager").arguments("<name> <profession> <type> <level>")
            .description("Set villager type")
            .completers(spawnNameCompleter,
                        CommandArgCompleter.enumLowerList(Villager.Profession.class),
                        CommandArgCompleter.enumLowerList(Villager.Type.class),
                        CommandArgCompleter.integer(i -> i >= 1 && i <= 5))
            .senderCaller(this::spawnSetVillager);
        spawnNode.addChild("bring").arguments("<name>")
            .completers(spawnNameCompleter)
            .description("Bring spawn to current location")
            .playerCaller(this::spawnBring);
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
        String name = args[0];
        MerchantFile merchantFile = plugin.merchants.merchantFileMap.get(name);
        if (merchantFile == null) {
            merchantFile = new MerchantFile(name);
        }
        RecipeMakerMenu menu = new RecipeMakerMenu(name);
        final int maxUses = args.length >= 2
            ? intOf(args[1])
            : -1;
        Inventory inventory = plugin.getServer()
            .createInventory(menu, 9, Component.text("New Merchant Recipe"));
        menu.merchantFile = merchantFile;
        menu.inventory = inventory;
        menu.maxUses = maxUses;
        player.openInventory(inventory);
        return true;
    }

    boolean recipeList(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("" + plugin.merchants.merchantFileMap.size() + " merchants:",
                                              NamedTextColor.YELLOW));
            for (String name : plugin.merchants.merchantFileMap.keySet()) {
                sender.sendMessage(Component.text("- " + name, NamedTextColor.YELLOW));
            }
            return true;
        }
        if (args.length != 1) return false;
        MerchantFile merchantFile = merchantFileOf(args[0]);
        int i = 0;
        sender.sendMessage(Component.text(merchantFile.getName() + " has " + merchantFile.getRecipes().size()
                                          + " recipes:", NamedTextColor.YELLOW));
        for (Recipe recipe : merchantFile.getRecipes()) {
            sender.sendMessage(Component.text("" + (i++) + ") " + Items.toString(recipe),
                                              NamedTextColor.YELLOW));
        }
        return true;
    }

    boolean recipeEdit(Player player, String[] args) {
        if (args.length < 2 || args.length > 3) return false;
        String name = args[0];
        int index = intOf(args[1]);
        final int maxUses = args.length >= 3
            ? intOf(args[2])
            : -1;
        MerchantFile merchantFile = merchantFileOf(name);
        Recipe recipe = recipeOf(merchantFile, index);
        RecipeMakerMenu menu = new RecipeMakerMenu(merchantFile.getName());
        menu.merchantFile = merchantFile;
        menu.recipe = recipe;
        Inventory inventory = plugin.getServer()
            .createInventory(menu, 9, Component.text("Edit Merchant Recipe"));
        inventory.setItem(0, Items.deserialize(recipe.getInA()));
        inventory.setItem(1, Items.deserialize(recipe.getInB()));
        inventory.setItem(2, Items.deserialize(recipe.getOut()));
        menu.inventory = inventory;
        menu.maxUses = maxUses;
        player.openInventory(inventory);
        return true;
    }

    boolean recipeDelete(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        String name = args[0];
        int index = intOf(args[1]);
        MerchantFile merchantFile = merchantFileOf(name);
        Recipe recipe = recipeOf(merchantFile, index);
        merchantFile.getRecipes().remove(recipe);
        plugin.merchants.saveMerchant(merchantFile);
        sender.sendMessage(Component.text("Deleted recipe " + merchantFile.getName() + "/" + index + ": " + Items.toString(recipe),
                                          NamedTextColor.YELLOW));
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

    boolean recipeMove(CommandSender sender, String[] args) {
        if (args.length != 3) return false;
        String name = args[0];
        MerchantFile merchantFile = merchantFileOf(name);
        int index1 = intOf(args[1]);
        int index2 = intOf(args[2]);
        if (index1 < 0 || index1 >= merchantFile.getRecipes().size()) {
            throw new CommandWarn("From-index out of bounds: " + index1 + "/"
                                  + (merchantFile.getRecipes().size() - 1));
        }
        Recipe recipe = merchantFile.getRecipes().remove(index1);
        index2 = Math.max(0, Math.min(merchantFile.getRecipes().size(), index2));
        merchantFile.getRecipes().add(index2, recipe);
        plugin.merchants.saveMerchant(merchantFile);
        sender.sendMessage(Component.text(merchantFile.getName() + ": Recipe moved from index "
                                          + index1 + " to " + index2 + ": "
                                          + Items.toString(recipe), NamedTextColor.YELLOW));
        return true;
    }

    boolean spawnCreate(Player player, String[] args) {
        if (args.length != 1 && args.length != 2) return false;
        MerchantFile merchantFile = merchantFileOf(args[0]);
        String name = args.length >= 2
            ? args[1]
            : merchantFile.getName();
        Spawn spawn = new Spawn();
        spawn.load(player.getLocation());
        spawn.setName(name);
        spawn.setMerchant(merchantFile.getName());
        plugin.merchants.spawnMap.put(name, spawn);
        plugin.merchants.saveSpawn(spawn);
        plugin.merchants.spawnAll();
        player.sendMessage(Component.text("Spawn " + name + " created at current location", NamedTextColor.YELLOW));
        return true;
    }

    boolean spawnList(CommandSender sender, String[] args) {
        int i = 0;
        sender.sendMessage(Component.text(plugin.merchants.spawnMap.size() + " spawns:", NamedTextColor.YELLOW));
        for (Spawn spawn : plugin.merchants.spawnMap.values()) {
            sender.sendMessage(Component.text("" + (i++) + ") "
                                              + spawn.simplified(),
                                              NamedTextColor.YELLOW));
        }
        return true;
    }

    boolean spawnDelete(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Spawn spawn = spawnOf(args[0]);
        plugin.merchants.deleteSpawn(spawn);
        plugin.merchants.clearMobs();
        plugin.merchants.spawnAll();
        sender.sendMessage(Component.text("Deleted spawn :" + spawn.simplified(), NamedTextColor.YELLOW));
        return true;
    }

    boolean reload(CommandSender sender, String[] args) {
        plugin.merchants.unload();
        plugin.merchants.load();
        sender.sendMessage(Component.text("Files reloaded", NamedTextColor.YELLOW));
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
        plugin.merchants.saveSpawn(spawn);
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
        appearance.setEntityType(EntityType.VILLAGER);
        try {
            appearance.setVillagerProfession(Villager.Profession.valueOf(args[1].toUpperCase()));
            appearance.setVillagerType(Villager.Type.valueOf(args[2].toUpperCase()));
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Invalid profession or type: " + args[1] + ", " + args[2]);
        }
        final int lvl = intOf(args[3]);
        if (lvl < 1 || lvl > 5) {
            throw new CommandWarn("Illegal villager level: " + lvl);
        }
        appearance.setVillagerLevel(lvl);
        spawn.setAppearance(appearance);
        plugin.merchants.saveSpawn(spawn);
        plugin.merchants.clearMobs();
        plugin.merchants.spawnAll();
        sender.sendMessage(Component.text("Villager appearance updated", NamedTextColor.YELLOW));
        return true;
    }

    protected boolean spawnBring(Player player, String[] args) {
        if (args.length < 1) return false;
        Spawn spawn = spawnOf(args[0]);
        spawn.load(player.getLocation());
        plugin.merchants.saveSpawn(spawn);
        player.sendMessage(Component.text("Brought spawn to current location", NamedTextColor.YELLOW));
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

    private MerchantFile merchantFileOf(final String arg) throws CommandWarn {
        MerchantFile result = plugin.merchants.merchantFileMap.get(arg);
        if (result == null) {
            throw new CommandWarn("Merchant not found: " + arg);
        }
        return result;
    }

    private Recipe recipeOf(MerchantFile merchantFile, int index) {
        if (index < 0 || index >= merchantFile.getRecipes().size()) {
            throw new CommandWarn("Invalid index: " + index);
        }
        return merchantFile.getRecipes().get(index);
    }

    private Spawn spawnOf(final String arg) throws CommandWarn {
        Spawn spawn = plugin.merchants.spawnMap.get(arg);
        if (spawn == null) {
            throw new CommandWarn("Spawn not found: " + arg);
        }
        return spawn;
    }
}
