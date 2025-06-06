package com.cavetale.merchant;

import com.cavetale.core.event.player.PlayerInteractNpcEvent;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.util.Json;
import com.cavetale.merchant.save.MerchantFile;
import com.cavetale.merchant.save.Recipe;
import com.cavetale.merchant.save.Spawn;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsCategory;
import com.cavetale.mytems.MytemsPlugin;
import com.cavetale.mytems.item.coin.BankTeller;
import com.cavetale.mytems.item.combinable.ItemCombinerMenu;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class Merchants implements Listener {
    final MerchantPlugin plugin;
    protected final Map<String, MerchantFile> merchantFileMap = new HashMap<>();
    protected final Map<String, Spawn> spawnMap = new HashMap<>();
    private final Map<Spawn, UUID> spawnMobMap = new IdentityHashMap<>();
    private final Map<UUID, Spawn> uuidSpawnMap = new HashMap<>();
    private final Map<UUID, List<Recipe>> openMerchants = new HashMap<>();
    private File merchantsFolder;
    private File spawnsFolder;
    private boolean spawning;
    public static final List<String> SPECIAL_NAMES = List.of("Repairman",
                                                             "Maypole",
                                                             "PlayerHead",
                                                             "BankTeller");

    protected void enable() {
        merchantsFolder = new File(plugin.getDataFolder(), "merchants");
        spawnsFolder = new File(plugin.getDataFolder(), "spawns");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        load();
        spawnAll();
    }

    protected void disable() {
        unload();
    }

    protected void load() {
        for (File file : dir(merchantsFolder)) {
            if (!file.isFile()) continue;
            String name = file.getName();
            if (!name.endsWith(".json")) continue;
            name = name.substring(0, name.length() - 5);
            MerchantFile merchantFile = Json.load(file, MerchantFile.class);
            if (merchantFile == null) {
                plugin.getLogger().warning("Invalid merchant file: " + file);
                continue;
            }
            merchantFile.setName(name);
            merchantFile.fix();
            merchantFileMap.put(name, merchantFile);
        }
        for (File file : dir(spawnsFolder)) {
            if (!file.isFile()) continue;
            String name = file.getName();
            if (!name.endsWith(".json")) continue;
            name = name.substring(0, name.length() - 5);
            Spawn spawn = Json.load(file, Spawn.class);
            if (spawn == null) {
                plugin.getLogger().warning("Invalid spawn file: " + file);
                continue;
            }
            spawn.setName(name);
            spawnMap.put(name, spawn);
        }
    }

    private static List<File> dir(File dir) {
        File[] files = dir.listFiles();
        return files != null ? List.of(files) : List.of();
    }

    protected void unload() {
        clearMobs();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!openMerchants.containsKey(player.getUniqueId())) continue;
            player.closeInventory();
        }
        openMerchants.clear();
        merchantFileMap.clear();
        spawnMap.clear();
    }

    protected void saveMerchant(MerchantFile merchantFile) {
        merchantsFolder.mkdirs();
        File file = new File(merchantsFolder, merchantFile.getName() + ".json");
        Json.save(file, merchantFile, true);
    }

    protected void saveSpawn(Spawn spawn) {
        spawnsFolder.mkdirs();
        File file = new File(spawnsFolder, spawn.getName() + ".json");
        Json.save(file, spawn, true);
    }

    protected void deleteSpawn(Spawn spawn) {
        spawnMap.remove(spawn.getName());
        File file = new File(spawnsFolder, spawn.getName() + ".json");
        file.delete();
    }

    /**
     * If the ItemStack is a Mytem which asks for auto fixing, return
     * the newly created Mytem. If not, return the input.
     */
    public static ItemStack ifMytems(ItemStack in, Player player) {
        if (in == null) return null;
        Mytems mytems = Mytems.forItem(in);
        if (mytems == null) return in;
        String serialized = mytems.serializeItem(in);
        ItemStack res = Mytems.deserializeItem(serialized);
        return res;
    }

    public MerchantRecipe createMerchantRecipe(Player player, Recipe recipe) {
        ItemStack a = Items.deserialize(recipe.getInA());
        ItemStack b = Items.deserialize(recipe.getInB());
        ItemStack c = Items.deserialize(recipe.getOut());
        a = ifMytems(a, player);
        b = ifMytems(b, player);
        c = ifMytems(c, player);
        List<ItemStack> ins = new ArrayList<>(2);
        ins.add(a);
        if (b != null) ins.add(b);
        int maxUses;
        int uses;
        MerchantFile merchantFile = merchantFileMap.get(recipe.getMerchant());
        if (recipe.getMaxUses() >= 0) {
            maxUses = recipe.getMaxUses();
            uses = merchantFile != null && merchantFile.isPersistent()
                ? plugin.users.getRecipeUses(player.getUniqueId(), recipe)
                : 0;
        } else {
            maxUses = 999;
            uses = 0;
        }
        MerchantRecipe result = new MerchantRecipe(c, maxUses);
        result.setUses(uses);
        result.setIngredients(ins);
        return result;
    }

    protected Merchant createRepairman(Player player, Component displayName) {
        Merchant merchant = plugin.getServer().createMerchant(displayName);
        List<MerchantRecipe> list = new ArrayList<>();
        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getAmount() == 0 || !item.hasItemMeta()) continue;
            if (Mytems.forItem(item) != null) continue;
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
            MerchantRecipe recipe = new MerchantRecipe(output, 1);
            List<ItemStack> ins = new ArrayList<>(2);
            ins.add(item.clone());
            final int price = (dmg - 1) / 512 + 1;
            ins.add(Mytems.RUBY.createItemStack(price));
            recipe.setIngredients(ins);
            list.add(recipe);
        }
        merchant.setRecipes(list);
        return merchant;
    }

    protected Merchant createPlayerHeadSalesman(Player player, Component displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        head.editMeta(meta -> ((SkullMeta) meta).setPlayerProfile(player.getPlayerProfile()));
        MerchantRecipe recipe = new MerchantRecipe(head, 999);
        recipe.setIngredients(List.of(new ItemStack(Material.BONE_BLOCK), Mytems.RUBY.createItemStack(2)));
        Merchant merchant = plugin.getServer().createMerchant(displayName);
        merchant.setRecipes(List.of(recipe));
        return merchant;
    }

    protected void clearMobs() {
        for (UUID uuid : new ArrayList<>(spawnMobMap.values())) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
        spawnMobMap.clear();
        uuidSpawnMap.clear();
    }

    protected void spawnAll() {
        for (Spawn spawn : spawnMap.values()) {
            tryToSpawn(spawn);
        }
    }

    protected void tryToSpawn(Spawn spawn) {
        if (spawnMobMap.containsKey(spawn)) return;
        Location loc = spawn.toLocation();
        if (loc == null) return;
        if (!loc.isChunkLoaded()) return;
        final Villager villager = spawn.spawn();
        if (villager == null) {
            plugin.getLogger().info("Failed to spawn: " + spawn.simplified());
            return;
        }
        villager.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0);
        villager.setPersistent(false);
        villager.setCollidable(false);
        villager.setSilent(true);
        Bukkit.getMobGoals().removeAllGoals(villager);
        spawnMobMap.put(spawn, villager.getUniqueId());
        uuidSpawnMap.put(villager.getUniqueId(), spawn);
        plugin.getLogger().info("Spawned: " + spawn.simplified());
    }

    protected Spawn spawnOf(Entity entity) {
        return uuidSpawnMap.get(entity.getUniqueId());
    }

    protected InventoryView openMerchant(Player player, Spawn spawn) {
        Component displayName = spawn.getDisplayName();
        return displayName != null && !empty().equals(displayName)
            ? openMerchant(player, spawn.getMerchant(), displayName)
            : openMerchant(player, spawn.getMerchant(), text(spawn.getMerchant()));
    }

    /**
     * Open the named merchant and keep track of it if necessary.
     * See onClose().
     * @return the resulting InventoryView
     */
    protected InventoryView openMerchant(Player player, String name, Component displayName) {
        MytemsPlugin.getInstance().fixPlayerInventory(player);
        switch (name) {
        case "Repairman":
            return player.openMerchant(createRepairman(player, displayName), true);
        case "PlayerHead":
            return player.openMerchant(createPlayerHeadSalesman(player, displayName), true);
        case "BankTeller":
            BankTeller.open(player);
            return player.getOpenInventory();
        case "Combiner":
            new ItemCombinerMenu(player).open();
            return player.getOpenInventory();
        case "Furniture:Sofa":
            return new FurnitureMenu(player, MytemsCategory.SOFA, 10).open();
        case "Furniture:Armchair":
            return new FurnitureMenu(player, MytemsCategory.ARMCHAIR, 5).open();
        case "Furniture:Chair":
            return new FurnitureMenu(player, MytemsCategory.CHAIR, 5).open();
        default:
            break;
        }
        MerchantFile merchantFile = merchantFileMap.get(name);
        if (merchantFile == null) return null;
        Merchant merchant = plugin.getServer().createMerchant(displayName);
        List<MerchantRecipe> merchantRecipeList = new ArrayList<>();
        List<Recipe> recipeList = new ArrayList<>();
        for (Recipe recipe : merchantFile.getRecipes()) {
            MerchantRecipe merchantRecipe = createMerchantRecipe(player, recipe);
            merchantRecipeList.add(merchantRecipe);
            recipeList.add(recipe);
        }
        merchant.setRecipes(merchantRecipeList);
        openMerchants.put(player.getUniqueId(), recipeList);
        return player.openMerchant(merchant, true);
    }

    private void onClose(Player player, MerchantInventory inventory) {
        List<Recipe> recipeList = openMerchants.remove(player.getUniqueId());
        if (recipeList == null) return;
        List<MerchantRecipe> merchantRecipeList = inventory.getMerchant().getRecipes();
        if (recipeList.size() != merchantRecipeList.size()) {
            plugin.getLogger().warning("Merchants::onClose: Sizes don't match: recipeList=" + recipeList + ", merchantRecipeList=" + merchantRecipeList);
        }
        int max = Math.min(recipeList.size(), merchantRecipeList.size());
        MerchantFile merchantFile = merchantFileMap.get(recipeList.get(0).getMerchant());
        if (merchantFile != null && merchantFile.isPersistent()) {
            for (int i = 0; i < max; i += 1) {
                Recipe recipe = recipeList.get(i);
                if (recipe.getMaxUses() < 0) continue;
                MerchantRecipe merchantRecipe = merchantRecipeList.get(i);
                plugin.users.setRecipeUses(player.getUniqueId(), recipe, merchantRecipe.getUses());
                plugin.users.save(player.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        final Entity entity = event.getEntity();
        Spawn spawn = uuidSpawnMap.remove(entity.getUniqueId());
        if (spawn == null) return;
        spawnMobMap.remove(spawn);
        Bukkit.getScheduler().runTask(plugin, () -> entity.remove());
        plugin.getLogger().info("Removed from World: " + spawn.simplified());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onWorldUnload(WorldUnloadEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::checkSpawnValidity);
    }

    private void checkSpawnValidity() {
        for (Iterator<Map.Entry<Spawn, UUID>> iter = spawnMobMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<Spawn, UUID> it = iter.next();
            UUID uuid = it.getValue();
            if (Bukkit.getEntity(uuid) != null) continue;
            Spawn spawn = it.getKey();
            iter.remove();
            uuidSpawnMap.remove(uuid);
            plugin.getLogger().info("Spawn disappeared: " + spawn.simplified());
        }
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

    @EventHandler(priority = EventPriority.LOWEST)
    void onEntityMove(EntityMoveEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        Mob mob = (Mob) event.getEntity();
        Spawn spawn = uuidSpawnMap.get(mob.getUniqueId());
        if (spawn == null) return;
        if (!spawn.isNearby(mob.getLocation(), 1.0)) {
            mob.teleport(spawn.toLocation());
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Spawn spawn = spawnOf(entity);
        if (spawn == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (new PlayerInteractNpcEvent(player, spawn.getMerchant(), entity).callEvent()) {
            openMerchant(player, spawn);
            plugin.getLogger().info(player.getName() + " opened " + spawn.getMerchant());
        }
        PluginPlayerEvent.Name.INTERACT_NPC.make(plugin, player)
            .detail(Detail.NAME, spawn.getMerchant())
            .callEvent();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        Spawn spawn = spawnOf(entity);
        if (spawn == null) return;
        entity.setPersistent(false);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
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
            recipe.setMerchant(menu.merchant);
            recipe.setInA(Items.serialize(a));
            recipe.setInB(Items.serialize(b));
            recipe.setOut(Items.serialize(c));
            recipe.setMaxUses(menu.maxUses);
            if (menu.recipe == null) {
                // New recipe
                merchantFileMap.put(menu.merchantFile.getName(), menu.merchantFile);
                menu.merchantFile.getRecipes().add(recipe);
                menu.merchantFile.fix();
                saveMerchant(menu.merchantFile);
                player.sendMessage(textOfChildren(text("New recipe created: ", YELLOW),
                                                  recipe.toComponent()));
            } else {
                // Editing
                saveMerchant(menu.merchantFile);
                player.sendMessage(textOfChildren(text("Recipe edited: ", YELLOW),
                                                  recipe.toComponent()));
            }
        } else if (inv instanceof MerchantInventory) {
            onClose(player, (MerchantInventory) inv);
        }
    }
}
