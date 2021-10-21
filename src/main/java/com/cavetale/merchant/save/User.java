package com.cavetale.merchant.save;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * JSONable.
 * This is stored in the users/UUID.json files.
 */
@Data
public final class User {
    protected Map<String, Map<Long, Integer>> recipes = new HashMap<>();

    public int getRecipeUses(String name, long id) {
        Map<Long, Integer> map = recipes.get(name);
        if (map == null) return 0;
        Integer result = map.get(id);
        return result != null ? result : 0;
    }

    public void setRecipeUses(String name, long id, int uses) {
        Map<Long, Integer> map = recipes.computeIfAbsent(name, n -> new HashMap<>());
        map.put(id, uses);
    }
}
