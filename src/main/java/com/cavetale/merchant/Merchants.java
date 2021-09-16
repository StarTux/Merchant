package com.cavetale.merchant;

import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsPlugin;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

@RequiredArgsConstructor
public final class Merchants implements Listener {
    final MerchantPlugin plugin;
    Recipes recipes;
    private final Map<Spawn, Mob> spawnMobMap = new IdentityHashMap<>();
    private final Map<Integer, Spawn> idSpawnMap = new HashMap<>();
    private final Map<UUID, List<Recipe>> openMerchants = new HashMap<>();
    static final String PATH = "recipes.json";
    private boolean spawning;

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

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        load();
        spawnAll();
    }

    protected void disable() {
        unload();
    }

    protected void load() {
        clearMobs();
        File file = new File(plugin.getDataFolder(), PATH);
        recipes = Json.load(file, Recipes.class, Recipes::new);
        if (recipes.fix()) save();
    }

    protected void unload() {
        clearMobs();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!openMerchants.containsKey(player.getUniqueId())) continue;
            player.closeInventory();
        }
        openMerchants.clear();
    }

    protected void save() {
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

    protected Merchant createRepairman(Player player, String name) {
        Merchant merchant = plugin.getServer().createMerchant(Component.text(name));
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

    protected Merchant createPlayerHeadSalesman(Player player, String name) {
        Merchant merchant = plugin.getServer().createMerchant(Component.text(name));
        List<MerchantRecipe> list = new ArrayList<>();
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(player.getPlayerProfile());
        head.setItemMeta((ItemMeta) meta);
        MerchantRecipe recipe = new MerchantRecipe(head, 999);
        List<ItemStack> ins = new ArrayList<>(2);
        ins.add(Mytems.KITTY_COIN.createItemStack(player));
        recipe.setIngredients(ins);
        list.add(recipe);
        merchant.setRecipes(list);
        return merchant;
    }

    protected void clearMobs() {
        for (Mob mob : new ArrayList<>(spawnMobMap.values())) {
            mob.remove();
        }
        spawnMobMap.clear();
        idSpawnMap.clear();
    }

    protected Villager.Profession randomProfession(List<Villager.Profession> list) {
        return list.get(plugin.random.nextInt(list.size()));
    }

    protected void spawnAll() {
        for (Spawn spawn : recipes.spawns) {
            tryToSpawn(spawn);
        }
    }

    protected void tryToSpawn(Spawn spawn) {
        if (spawnMobMap.containsKey(spawn)) return;
        Location loc = spawn.toLocation();
        if (loc == null) return;
        if (!loc.isChunkLoaded()) return;
        Villager villager = loc.getWorld().spawn(loc, Villager.class, v -> {
                v.setPersistent(false);
                if (spawn.merchant.equals("Repairman")) {
                    v.setProfession(Villager.Profession.WEAPONSMITH);
                } else if (spawn.merchant.equals("Maypole")) {
                    v.setProfession(Villager.Profession.LIBRARIAN);
                } else if (spawn.merchant.equals("PlayerHead")) {
                    v.setProfession(Villager.Profession.CARTOGRAPHER);
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
                v.setCollidable(false);
            });
        if (villager == null) {
            plugin.getLogger().info("Failed to spawn: " + spawn.simplified());
            return;
        }
        Bukkit.getMobGoals().removeAllGoals(villager);
        spawnMobMap.put(spawn, villager);
        idSpawnMap.put(villager.getEntityId(), spawn);
        plugin.getLogger().info("Spawned: " + spawn.simplified());
    }

    protected Spawn spawnOf(Entity entity) {
        return idSpawnMap.get(entity.getEntityId());
    }

    /**
     * Open the named merchant and keep track of it if necessary.
     * See onClose().
     * @return the resulting InventoryView
     */
    InventoryView openMerchant(Player player, String name) {
        MytemsPlugin.getInstance().fixPlayerInventory(player);
        Merchant merchant = plugin.getServer().createMerchant(Component.text(name));
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

    @EventHandler
    void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        int entityId = event.getEntity().getEntityId();
        Spawn spawn = idSpawnMap.remove(entityId);
        if (spawn == null) return;
        spawnMobMap.remove(spawn);
    }


    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (spawning) return;
        spawning = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                spawning = false;
                spawnAll();
            }, 20L);
    }

    @EventHandler
    void onEntityMove(EntityMoveEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        Mob mob = (Mob) event.getEntity();
        Spawn spawn = idSpawnMap.get(mob.getEntityId());
        if (spawn == null) return;
        if (!spawn.isNearby(mob.getLocation(), 1.0)) {
            mob.teleport(spawn.toLocation());
        }
    }

    @EventHandler
    void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Spawn spawn = spawnOf(entity);
        if (spawn == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if ("Repairman".equals(spawn.merchant)) {
            Merchant merchant = createRepairman(player, spawn.merchant);
            player.openMerchant(merchant, false);
        } else if ("PlayerHead".equals(spawn.merchant)) {
            Merchant merchant = createPlayerHeadSalesman(player, spawn.merchant);
            player.openMerchant(merchant, false);
        } else {
            openMerchant(player, spawn.merchant);
        }
        plugin.getLogger().info(player.getName() + " opened " + spawn.merchant);
        PluginPlayerEvent.Name.INTERACT_NPC.ultimate(plugin, player)
            .detail(Detail.NAME, spawn.merchant)
            .call();
    }

    @EventHandler
    void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        Spawn spawn = spawnOf(entity);
        if (spawn == null) return;
        entity.setPersistent(false);
        event.setCancelled(true);
    }

    @EventHandler
    void onInventoryClose(InventoryCloseEvent event) {
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
                addRecipe(recipe);
                save();
                player.sendMessage(Component.text("New recipe created: " + Items.toString(recipe),
                                                  NamedTextColor.YELLOW));
            } else {
                save();
                player.sendMessage(Component.text("Recipe edited: " + Items.toString(recipe),
                                                  NamedTextColor.YELLOW));
            }
        } else if (inv instanceof MerchantInventory) {
            onClose(player, (MerchantInventory) inv);
        }
    }
}
