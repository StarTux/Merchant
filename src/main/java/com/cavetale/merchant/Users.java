package com.cavetale.merchant;

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

    public void setRecipeUses(UUID uuid, long id, int uses) {
        of(uuid).recipeUses.put(id, uses);
    }

    public int getRecipeUses(UUID uuid, long id) {
        Integer result = of(uuid).recipeUses.get(id);
        return result != null ? result : 0;
    }

    public void clear() {
        cache.clear();
    }
}
