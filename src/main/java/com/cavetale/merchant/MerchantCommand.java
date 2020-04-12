package com.cavetale.merchant;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@RequiredArgsConstructor
final class MerchantCommand implements TabExecutor {
    private final MerchantPlugin plugin;

    static class Wrong extends Exception {
        Wrong(final String msg) {
            super(msg);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String alias, String[] args) {
        if (args.length == 0) return false;
        try {
            return onCommand(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
        } catch (Wrong w) {
            sender.sendMessage(ChatColor.RED + w.getMessage());
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 0) return null;
        String cmd = args[0];
        if (args.length == 1) {
            return complete(cmd, Stream.of("create", "list", "delete", "open",
                                           "spawn", "listspawns", "deletespawn",
                                           "reload", "save"));
        }
        String arg = args[args.length - 1];
        return null;
    }

    boolean onCommand(CommandSender sender, String cmd, String[] args) throws Wrong {
        switch (cmd) {
        case "create": {
            if (args.length != 1) return false;
            Player player = playerOf(sender);
            RecipeMakerMenu menu = new RecipeMakerMenu(args[0]);
            Inventory inventory = plugin.getServer()
                .createInventory(menu, 9, "New Merchant Recipe");
            menu.inventory = inventory;
            player.openInventory(inventory);
            return true;
        }
        case "list": {
            int i = 0;
            for (Recipe recipe : plugin.merchants.recipes.recipes) {
                sender.sendMessage("" + (i++) + ") "
                                   + recipe.merchant + ": "
                                   + Items.toString(recipe));
            }
            sender.sendMessage(i + " total recipes");
            return true;
        }
        case "delete": {
            if (args.length != 1) return false;
            int i = intOf(args[0]);
            if (i < 0 || i >= plugin.merchants.recipes.recipes.size()) {
                throw new Wrong("Illegal index: " + i);
            }
            Recipe recipe = plugin.merchants.recipes.recipes.remove(i);
            plugin.merchants.save();
            sender.sendMessage("Deleted recipe #" + i + ": " + Items.toString(recipe));
            return true;
        }
        case "edit": {
            if (args.length != 1) return false;
            Player player = playerOf(sender);
            int i = intOf(args[0]);
            if (i < 0 || i >= plugin.merchants.recipes.recipes.size()) {
                throw new Wrong("Illegal index: " + i);
            }
            Recipe recipe = plugin.merchants.recipes.recipes.get(i);
            RecipeMakerMenu menu = new RecipeMakerMenu(recipe.merchant);
            menu.recipe = recipe;
            Inventory inventory = plugin.getServer()
                .createInventory(menu, 9, "Edit Merchant Recipe");
            menu.inventory = inventory;
            player.openInventory(inventory);
            return true;
        }
        case "open": {
            if (args.length != 1 && args.length != 2) return false;
            Player target;
            if (args.length >= 2) {
                String name = args[1];
                target = plugin.getServer().getPlayer(name);
                if (target == null) throw new Wrong("Player not found: " + name);
            } else {
                target = playerOf(sender);
            }
            String merchant = args[0];
            target.openMerchant(plugin.merchants.createMerchant(merchant), false);
            sender.sendMessage(target.getName() + " opened merchant " + merchant);
            return true;
        }
        case "spawn": {
            if (args.length != 1) return false;
            String merchant = args[0];
            Player player = playerOf(sender);
            Spawn spawn = new Spawn();
            spawn.load(player.getLocation());
            spawn.merchant = merchant;
            plugin.merchants.recipes.spawns.add(spawn);
            plugin.merchants.save();
            sender.sendMessage(merchant + " spawn created at current location.");
            return true;
        }
        case "listspawns": {
            int i = 0;
            for (Spawn spawn : plugin.merchants.recipes.spawns) {
                sender.sendMessage("" + (i++) + ") "
                                   + spawn.simplified());
            }
            sender.sendMessage(i + " total spawns");
            return true;
        }
        case "deletespawn": {
            if (args.length != 1) return false;
            int i = intOf(args[0]);
            if (i < 0 || i >= plugin.merchants.recipes.spawns.size()) {
                throw new Wrong("Illegal index: " + i);
            }
            Spawn spawn = plugin.merchants.recipes.spawns.remove(i);
            plugin.merchants.save();
            plugin.merchants.clearMobs();
            sender.sendMessage("Deleted spawn #" + i + ": " + spawn.simplified());
            return true;
        }
        case "reload": {
            plugin.merchants.load();
            sender.sendMessage("Recipes reloaded");
            return true;
        }
        case "save": {
            plugin.merchants.save();
            sender.sendMessage("Recipes saved");
            return true;
        }
        default:
            return false;
        }
    }

    private List<String> complete(final String arg, final Stream<String> opt) {
        return opt.filter(o -> o.startsWith(arg))
            .collect(Collectors.toList());
    }

    Player playerOf(final CommandSender sender) throws Wrong {
        if (!(sender instanceof Player)) {
            throw new Wrong("Player expected");
        }
        return (Player) sender;
    }

    int intOf(final String arg) throws Wrong {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            throw new Wrong("Not a number: " + arg);
        }
    }
}
