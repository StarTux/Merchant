package com.cavetale.merchant;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.util.Json;
import com.cavetale.merchant.save.MerchantFile;
import com.cavetale.merchant.save.Recipe;
import com.cavetale.merchant.save.Spawn;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

final class MerchantCommand extends AbstractCommand<MerchantPlugin> {
    MerchantCommand(final MerchantPlugin plugin) {
        super(plugin, "merchant");
    }

    @Override
    protected void onEnable() {
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
        recipeNode.addChild("clone").arguments("<merchant> <num> [maxUses]")
            .completers(merchantFileNameCompleter,
                        CommandArgCompleter.integer(i -> i >= 0),
                        CommandArgCompleter.integer(i -> i >= -1))
            .description("Clone recipe")
            .playerCaller(this::recipeClone);
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
                        CommandArgCompleter.keyedLowerList(RegistryKey.VILLAGER_PROFESSION),
                        CommandArgCompleter.keyedLowerList(RegistryKey.VILLAGER_TYPE),
                        CommandArgCompleter.integer(i -> i >= 1 && i <= 5))
            .senderCaller(this::spawnSetVillager);
        spawnNode.addChild("bring").arguments("<name>")
            .completers(spawnNameCompleter)
            .description("Bring spawn to current location")
            .playerCaller(this::spawnBring);
        // merchant
        CommandNode merchantNode = rootNode.addChild("merchant")
            .description("Merchant options");
        merchantNode.addChild("list").denyTabCompletion()
            .description("List all merchants")
            .senderCaller(this::merchantList);
        merchantNode.addChild("persistent").arguments("<merchant> true|false")
            .description("Update persistence of a merchant")
            .completers(merchantFileNameCompleter,
                        CommandArgCompleter.list("true", "false"))
            .senderCaller(this::merchantPersistent);
        // Spawn
        rootNode.addChild("spawnforreal").arguments("<name> <profession> <type> <level>")
            .completers(merchantFileNameCompleter,
                        CommandArgCompleter.keyedLowerList(RegistryKey.VILLAGER_PROFESSION),
                        CommandArgCompleter.keyedLowerList(RegistryKey.VILLAGER_TYPE),
                        CommandArgCompleter.integer(i -> i >= 1 && i <= 5))
            .playerCaller(this::spawnForReal);
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
            .createInventory(menu, 9, text("New Merchant Recipe"));
        menu.merchantFile = merchantFile;
        menu.inventory = inventory;
        menu.maxUses = maxUses;
        player.openInventory(inventory);
        return true;
    }

    boolean recipeList(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(text("" + plugin.merchants.merchantFileMap.size() + " merchants:",
                                    YELLOW));
            for (String name : plugin.merchants.merchantFileMap.keySet()) {
                sender.sendMessage(text("- " + name, YELLOW));
            }
            return true;
        }
        if (args.length != 1) return false;
        MerchantFile merchantFile = merchantFileOf(args[0]);
        int i = 0;
        sender.sendMessage(text(merchantFile.getName() + " has " + merchantFile.getRecipes().size()
                                + " recipes:", YELLOW));
        for (Recipe recipe : merchantFile.getRecipes()) {
            sender.sendMessage(textOfChildren(text("" + (i++) + ") ", YELLOW), recipe.toTradeComponent()));
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
            .createInventory(menu, 9, text("Edit Merchant Recipe"));
        inventory.setItem(0, Items.deserialize(recipe.getInA()));
        inventory.setItem(1, Items.deserialize(recipe.getInB()));
        inventory.setItem(2, Items.deserialize(recipe.getOut()));
        menu.inventory = inventory;
        menu.maxUses = maxUses;
        player.openInventory(inventory);
        return true;
    }

    private boolean recipeClone(Player player, String[] args) {
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
        Inventory inventory = plugin.getServer()
            .createInventory(menu, 9, text("Cloned Merchant Recipe"));
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
        sender.sendMessage(textOfChildren(text("Deleted recipe " + merchantFile.getName() + "/" + index + ": ", YELLOW),
                                          recipe.toTradeComponent()));
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
        plugin.merchants.openMerchant(target, merchant, text(merchant));
        sender.sendMessage(text(target.getName() + " opened merchant " + merchant, YELLOW));
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
        sender.sendMessage(textOfChildren(text(merchantFile.getName() + ": Recipe moved from index "
                                               + index1 + " to " + index2 + ": ", YELLOW),
                                          recipe.toTradeComponent()));
        return true;
    }

    boolean spawnCreate(Player player, String[] args) {
        if (args.length != 1 && args.length != 2) return false;
        String merchantName = args[0];
        if (!Merchants.SPECIAL_NAMES.contains(merchantName) && plugin.merchants.merchantFileMap.get(merchantName) == null) {
            player.sendMessage(text("Warning: Unknown merchant: " + merchantName));
        }
        String spawnName = args.length >= 2 ? args[1] : args[0];
        if (plugin.merchants.spawnMap.get(spawnName) != null) {
            throw new CommandWarn("Spawn already exists: " + spawnName);
        }
        Spawn spawn = new Spawn();
        spawn.load(player.getLocation());
        spawn.setName(spawnName);
        spawn.setMerchant(merchantName);
        plugin.merchants.spawnMap.put(spawnName, spawn);
        plugin.merchants.saveSpawn(spawn);
        plugin.merchants.spawnAll();
        player.sendMessage(text("Spawn " + spawnName + " created at current location", YELLOW));
        return true;
    }

    boolean spawnList(CommandSender sender, String[] args) {
        int i = 0;
        sender.sendMessage(text(plugin.merchants.spawnMap.size() + " spawns:", YELLOW));
        for (Spawn spawn : plugin.merchants.spawnMap.values()) {
            sender.sendMessage(text("" + (i++) + ") "
                                    + spawn.simplified(),
                                    YELLOW));
        }
        return true;
    }

    boolean spawnDelete(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Spawn spawn = spawnOf(args[0]);
        plugin.merchants.deleteSpawn(spawn);
        plugin.merchants.clearMobs();
        plugin.merchants.spawnAll();
        sender.sendMessage(text("Deleted spawn: " + spawn.simplified(), YELLOW));
        return true;
    }

    boolean reload(CommandSender sender, String[] args) {
        plugin.merchants.unload();
        plugin.merchants.load();
        sender.sendMessage(text("Files reloaded", YELLOW));
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
        sender.sendMessage(text().content("Display name set to ").color(YELLOW)
                           .append(spawn.getDisplayName()));
        return true;
    }

    protected boolean spawnSetVillager(CommandSender sender, String[] args) {
        if (args.length != 4) return false;
        Spawn spawn = spawnOf(args[0]);
        Spawn.Appearance appearance = new Spawn.Appearance();
        appearance.setEntityType(EntityType.VILLAGER);
        try {
            final Villager.Profession profession = CommandArgCompleter.requireKeyed(Villager.Profession.class, RegistryKey.VILLAGER_PROFESSION, args[1]);
            final Villager.Type type = CommandArgCompleter.requireKeyed(Villager.Type.class, RegistryKey.VILLAGER_TYPE, args[2]);
            appearance.setVillagerProfession(profession.getKey().toString());
            appearance.setVillagerType(type.getKey().toString());
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
        sender.sendMessage(text("Villager appearance updated", YELLOW));
        return true;
    }

    protected boolean spawnBring(Player player, String[] args) {
        if (args.length < 1) return false;
        Spawn spawn = spawnOf(args[0]);
        spawn.load(player.getLocation());
        plugin.merchants.saveSpawn(spawn);
        player.sendMessage(text("Brought spawn to current location", YELLOW));
        return true;
    }

    protected boolean merchantList(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage(text("" + plugin.merchants.merchantFileMap.size() + " merchants:", YELLOW));
        for (String name : plugin.merchants.merchantFileMap.keySet()) {
            sender.sendMessage(text("- " + name, YELLOW));
        }
        return true;
    }

    protected boolean merchantPersistent(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        MerchantFile merchantFile = merchantFileOf(args[0]);
        boolean value;
        try {
            value = Boolean.parseBoolean(args[1]);
        } catch  (IllegalArgumentException iae) {
            throw new CommandWarn("Not a boolean: " + args[1]);
        }
        merchantFile.setPersistent(value);
        plugin.merchants.saveMerchant(merchantFile);
        sender.sendMessage(text("Persistence of " + merchantFile.getName() + " is now " + merchantFile.isPersistent(), YELLOW));
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

    private boolean spawnForReal(Player player, String[] args) {
        if (args.length != 4) return false;
        final String nameArg = args[0];
        final String professionArg = args[1];
        final String villagerTypeArg = args[2];
        final String levelArg = args[3];
        final MerchantFile merchantFile = plugin.merchants.merchantFileMap.get(nameArg);
        if (merchantFile == null) throw new CommandWarn("Merchant not found: " + nameArg);
        final Villager.Profession profession = CommandArgCompleter.requireKeyed(Villager.Profession.class, RegistryKey.VILLAGER_PROFESSION, professionArg);
        final Villager.Type type = CommandArgCompleter.requireKeyed(Villager.Type.class, RegistryKey.VILLAGER_TYPE, villagerTypeArg);
        final int level = CommandArgCompleter.requireInt(levelArg, i -> i >= 1 && i <= 5);
        Villager villager = player.getWorld().spawn(player.getLocation(), Villager.class, v -> {
                v.setAdult();
                v.setProfession(profession);
                v.setVillagerType(type);
                v.setVillagerLevel(level);
            });
        if (villager == null) throw new CommandWarn("Failed to spawn villager!");
        villager.setRecipes(merchantFile.toMerchantRecipeList());
        player.sendMessage(text("Villager spawned: " + villager.getUniqueId() + ", " + villager.getRecipes().size() + " recipe(s)", YELLOW)
                           .insertion(villager.getUniqueId().toString()));
        return true;
    }
}
