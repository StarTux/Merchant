package com.cavetale.merchant;

import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsPlugin;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

@RequiredArgsConstructor
public final class Merchants implements Runnable {
    final MerchantPlugin plugin;
    Recipes recipes;
    final Map<Spawn, Mob> mobs = new HashMap<>();
    private final Map<UUID, List<Recipe>> openMerchants = new HashMap<>();
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
        File file = new File(plugin.getDataFolder(), PATH);
        recipes = Json.load(file, Recipes.class, Recipes::new);
        if (recipes.fix()) save();
    }

    public void unload() {
        clearMobs();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!openMerchants.containsKey(player.getUniqueId())) continue;
            player.closeInventory();
        }
        openMerchants.clear();
    }

    public void save() {
        File file = new File(plugin.getDataFolder(), PATH);
        Json.save(file, recipes, true);
    }

    /**
     * If the ItemStack is a Mytem which asks for auto fixing, return
     * the newly created Mytem. If not, return the input.
     */
    public static ItemStack ifMytems(@Nullable ItemStack in, Player player) {
        if (in == null) return null;
        Mytems mytems = Mytems.forItem(in);
        if (mytems == null) return in;
        String serialized = mytems.serializeItem(in);
        ItemStack res = Mytems.deserializeItem(serialized, player);
        return res;
    }

    public MerchantRecipe createMerchantRecipe(Player player, Recipe recipe) {
        ItemStack a = Items.deserialize(recipe.inA);
        ItemStack b = Items.deserialize(recipe.inB);
        ItemStack c = Items.deserialize(recipe.out);
        a = ifMytems(a, player);
        b = ifMytems(b, player);
        c = ifMytems(c, player);
        List<ItemStack> ins = new ArrayList<>(2);
        ins.add(a);
        if (b != null) ins.add(b);
        int maxUses;
        int uses;
        if (recipe.maxUses >= 0) {
            maxUses = recipe.maxUses;
            uses = plugin.users.getRecipeUses(player.getUniqueId(), recipe.id);
        } else {
            maxUses = 999;
            uses = 0;
        }
        MerchantRecipe result = new MerchantRecipe(c, maxUses);
        result.setUses(uses);
        result.setIngredients(ins);
        return result;
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

    /**
     * Open the named merchant and keep track of it if necessary.
     * See onClose().
     * @return the resulting InventoryView
     */
    InventoryView openMerchant(Player player, String name) {
        MytemsPlugin.getInstance().fixPlayerInventory(player);
        Merchant merchant = plugin.getServer().createMerchant(name);
        List<MerchantRecipe> merchantRecipeList = new ArrayList<>();
        List<Recipe> recipeList = new ArrayList<>();
        for (Recipe recipe : recipes.recipes) {
            if (!name.equals(recipe.merchant)) continue;
            MerchantRecipe merchantRecipe = createMerchantRecipe(player, recipe);
            merchantRecipeList.add(merchantRecipe);
            recipeList.add(recipe);
        }
        merchant.setRecipes(merchantRecipeList);
        openMerchants.put(player.getUniqueId(), recipeList);
        return player.openMerchant(merchant, true);
    }

    void onClose(Player player, MerchantInventory inventory) {
        List<Recipe> recipeList = openMerchants.remove(player.getUniqueId());
        if (recipeList == null) return;
        List<MerchantRecipe> merchantRecipeList = inventory.getMerchant().getRecipes();
        if (recipeList.size() != merchantRecipeList.size()) {
            plugin.getLogger().warning("Merchants::onClose: Sizes don't match: recipeList=" + recipeList + ", merchantRecipeList=" + merchantRecipeList);
        }
        int max = Math.min(recipeList.size(), merchantRecipeList.size());
        for (int i = 0; i < max; i += 1) {
            Recipe recipe = recipeList.get(i);
            if (recipe.maxUses < 0) continue;
            MerchantRecipe merchantRecipe = merchantRecipeList.get(i);
            plugin.users.setRecipeUses(player.getUniqueId(), recipe.id, merchantRecipe.getUses());
            plugin.users.save(player.getUniqueId());
        }
    }

    public void addRecipe(Recipe recipe) {
        recipes.recipes.add(recipe);
        recipe.id = ++recipes.recipeId;
    }
}
