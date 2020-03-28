package com.cavetale.merchant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

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

    void clearMobs() {
        for (Mob mob : mobs.values()) mob.remove();
        mobs.clear();
    }

    Villager.Profession randomProfession() {
        return PROFESSIONS.get(plugin.random.nextInt(PROFESSIONS.size()));
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
            if (!spawn.isNearby(mob.getLocation(), 3.0)) {
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
                        v.setProfession(randomProfession());
                        v.setVillagerLevel(5);
                        Villager.Type[] types = Villager.Type.values();
                        Villager.Type type = types[plugin.random.nextInt(types.length)];
                        v.setVillagerType(type);
                        v.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
                        v.setRecipes(createMerchantRecipes(spawn.merchant));
                        v.setPersistent(false);
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
}
