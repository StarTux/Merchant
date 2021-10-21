package com.cavetale.merchant;

import com.cavetale.core.util.Json;
import com.cavetale.merchant.save.Recipe;
import com.cavetale.merchant.save.User;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Users manager. Files are stored in users/UUID.json.
 * User files are loaded on demand.
 */
@RequiredArgsConstructor
public final class Users {
    private final MerchantPlugin plugin;
    private final Map<UUID, User> cache = new HashMap<>();
    private File folder;

    public Users enable() {
        folder = new File(plugin.getDataFolder(), "users");
        folder.mkdirs();
        return this;
    }

    public User of(UUID uuid) {
        User cached = cache.get(uuid);
        if (cached != null) return cached;
        File file = new File(folder, uuid + ".json");
        User user = Json.load(file, User.class, User::new);
        cache.put(uuid, user);
        return user;
    }

    public void save(UUID uuid) {
        User user = cache.get(uuid);
        if (user == null) return;
        File file = new File(folder, uuid + ".json");
        Json.save(file, user, true);
    }

    public void setRecipeUses(UUID uuid, Recipe recipe, int uses) {
        of(uuid).setRecipeUses(recipe.getMerchant(), recipe.getId(), uses);
    }

    public int getRecipeUses(UUID uuid, Recipe recipe) {
        return of(uuid).getRecipeUses(recipe.getMerchant(), recipe.getId());
    }

    public void clear() {
        cache.clear();
    }
}
