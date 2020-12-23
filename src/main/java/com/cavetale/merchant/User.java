package com.cavetale.merchant;

import java.util.HashMap;
import java.util.Map;

/**
 * JSONable.
 * This is stored in the users/UUID.json files.
 */
public final class User {
    // Mapping Merchant.id to the uses.
    protected Map<Long, Integer> recipeUses = new HashMap<>();
}
