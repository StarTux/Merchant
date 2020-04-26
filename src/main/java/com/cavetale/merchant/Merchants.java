package com.cavetale.merchant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

@RequiredArgsConstructor
public final class Merchants implements Runnable {
    final MerchantPlugin plugin;
    Recipes recipes;
    final Map<Spawn, Mob> mobs = new HashMap<>();
    static final String PATH = "recipes.json";
    static final String META = "merchant:name";
    long ticks = 0;
    static final List<Villager.Profession> PROFESSIONS = Arrays
        .asList(Villager.Profession.ARMORER,
                Villager.Profession.BUTCHER,
                Villager.Profession.CARTOGRAPHER,
                Villager.Profession.CLERIC,
                Villager.Profession.FARMER,
                Villager.Profession.FISHERMAN,
                Villager.Profession.FLETCHER,
                Villager.Profession.LEATHERWORKER,
                Villager.Profession.LIBRARIAN,
                Villager.Profession.MASON,
                Villager.Profession.SHEPHERD,
                Villager.Profession.TOOLSMITH,
                Villager.Profession.WEAPONSMITH);

    public void load() {
        clearMobs();
        recipes = plugin.json.load(PATH, Recipes.class, Recipes::new);
    }

    public void save() {
        plugin.json.save(PATH, recipes, true);
    }

    public MerchantRecipe createMerchantRecipe(Recipe recipe) {
        ItemStack a = Items.deserialize(recipe.inA);
        ItemStack b = Items.deserialize(recipe.inB);
        ItemStack c = Items.deserialize(recipe.out);
        List<ItemStack> ins = new ArrayList<>(2);
        ins.add(a);
        if (b != null) ins.add(b);
        MerchantRecipe result = new MerchantRecipe(c, 999);
        result.setIngredients(ins);
        return result;
    }

    List<MerchantRecipe> createMerchantRecipes(String name) {
        List<MerchantRecipe> list = new ArrayList<>();
        for (Recipe recipe : recipes.recipes) {
            if (!name.equals(recipe.merchant)) continue;
            MerchantRecipe item = createMerchantRecipe(recipe);
            list.add(item);
        }
        return list;
    }

    Merchant createMerchant(String name) {
        Merchant merchant = plugin.getServer().createMerchant(name);
        merchant.setRecipes(createMerchantRecipes(name));
        return merchant;
    }

    Merchant createRepairman(Player player, String name) {
        Merchant merchant = plugin.getServer().createMerchant(name);
        List<MerchantRecipe> list = new ArrayList<>();
        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getAmount() == 0 || !item.hasItemMeta()) continue;
            final int max = item.getType().getMaxDurability();
            if (max <= 0) continue;
            if (!(item.getItemMeta() instanceof Damageable)) continue;
            ItemStack output = item.clone();
            Damageable meta = (Damageable) output.getItemMeta();
            if (!meta.hasDamage()) continue;
            final int dmg = meta.getDamage();
            if (dmg == 0) continue;
            meta.setDamage(0);
            output.setItemMeta((ItemMeta) meta);
            MerchantRecipe recipe = new MerchantRecipe(output, 999);
            List<ItemStack> ins = new ArrayList<>(2);
            ins.add(item.clone());
            final int price = Math.max(1, (dmg * 10) / max);
            ins.add(new ItemStack(Material.DIAMOND, price));
            recipe.setIngredients(ins);
            list.add(recipe);
        }
        merchant.setRecipes(list);
        return merchant;
    }

    void clearMobs() {
        for (Mob mob : mobs.values()) mob.remove();
        mobs.clear();
    }

    Villager.Profession randomProfession(List<Villager.Profession> list) {
        return list.get(plugin.random.nextInt(list.size()));
    }

    @Override
    public void run() {
        for (Iterator<Map.Entry<Spawn, Mob>> iter = mobs.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<Spawn, Mob> it = iter.next();
            Spawn spawn = it.getKey();
            Mob mob = it.getValue();
            if (!mob.isValid()) {
                iter.remove();
                continue;
            }
            if (!spawn.isNearby(mob.getLocation(), 1.0)) {
                mob.teleport(spawn.toLocation());
            }
        }
        if (ticks % 20 == 0) {
            for (Spawn spawn : recipes.spawns) {
                if (mobs.containsKey(spawn)) continue;
                Location loc = spawn.toLocation();
                if (loc == null) continue;
                if (!loc.isChunkLoaded()) continue;
                Villager villager = loc.getWorld().spawn(loc, Villager.class, v -> {
                        if (spawn.merchant.equals("Repairman")) {
                            v.setProfession(Villager.Profession.WEAPONSMITH);
                        } else if (spawn.merchant.equals("Maypole")) {
                            v.setProfession(Villager.Profession.LIBRARIAN);
                        } else {
                            v.setProfession(randomProfession(PROFESSIONS));
                        }
                        v.setVillagerLevel(5);
                        if (spawn.merchant.equals("Maypole")) {
                            v.setVillagerType(Villager.Type.PLAINS);
                        } else {
                            Villager.Type[] types = Villager.Type.values();
                            Villager.Type type = types[plugin.random.nextInt(types.length)];
                            v.setVillagerType(type);
                        }
                        v.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
                        v.setRecipes(createMerchantRecipes(spawn.merchant));
                        v.setPersistent(false);
                        v.setCollidable(false);
                    });
                if (villager == null) {
                    plugin.getLogger().info("Failed to spawn: " + spawn.simplified());
                    continue;
                }
                plugin.meta.set(villager, META, spawn.merchant);
                mobs.put(spawn, villager);
                plugin.getLogger().info("Spawned: " + spawn.simplified());
            }
        }
        ticks += 1;
    }

    Spawn spawnOf(Entity entity) {
        if (!(entity instanceof Mob)) return null;
        Mob mob = (Mob) entity;
        for (Map.Entry<Spawn, Mob> entry : mobs.entrySet()) {
            if (mob.equals(entry.getValue())) return entry.getKey();
        }
        return null;
    }
}
