package com.cavetale.merchant;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    final MerchantPlugin plugin;

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof RecipeMakerMenu) {
            RecipeMakerMenu menu = (RecipeMakerMenu) inv.getHolder();
            ItemStack a = Items.simplify(inv.getItem(0));
            ItemStack b = Items.simplify(inv.getItem(1));
            ItemStack c = Items.simplify(inv.getItem(2));
            if (c == null) return;
            if (a == null) return;
            Recipe recipe = menu.recipe != null
                ? menu.recipe
                : new Recipe();
            recipe.merchant = menu.merchant;
            recipe.inA = Items.serialize(a);
            recipe.inB = Items.serialize(b);
            recipe.out = Items.serialize(c);
            recipe.maxUses = menu.maxUses;
            if (menu.recipe == null) {
                plugin.merchants.addRecipe(recipe);
                plugin.merchants.save();
                player.sendMessage(ChatColor.YELLOW + "New recipe created: " + Items.toString(recipe));
            } else {
                plugin.merchants.save();
                player.sendMessage(ChatColor.YELLOW + "Recipe edited: " + Items.toString(recipe));
            }
        } else if (inv instanceof MerchantInventory) {
            plugin.merchants.onClose(player, (MerchantInventory) inv);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        String name = plugin.meta.get(entity, Merchants.META);
        if (name == null) return;
        // Found in the wild because of old mistakes?
        Spawn spawn = plugin.merchants.spawnOf(entity);
        if (spawn == null) {
            entity.remove();
            return;
        }
        // No return
        entity.setPersistent(false);
        event.setCancelled(true);
        Player player = event.getPlayer();
        if ("Repairman".equals(spawn.merchant)) {
            Merchant merchant = plugin.merchants.createRepairman(player, spawn.merchant);
            player.openMerchant(merchant, false);
        } else if ("PlayerHead".equals(spawn.merchant)) {
            Merchant merchant = plugin.merchants.createPlayerHeadSalesman(player, spawn.merchant);
            player.openMerchant(merchant, false);
        } else {
            plugin.merchants.openMerchant(player, spawn.merchant);
        }
        plugin.getLogger().info(player.getName() + " opened " + spawn.merchant);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        String name = plugin.meta.get(entity, Merchants.META);
        if (name == null) return;
        // Found in the wild because of old mistakes?
        Spawn spawn = plugin.merchants.spawnOf(entity);
        if (spawn == null) {
            entity.remove();
            return;
        }
        // Cancel
        entity.setPersistent(false);
        event.setCancelled(true);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            String name = plugin.meta.get(entity, Merchants.META);
            if (name == null) continue;
            entity.remove();
        }
    }
}
