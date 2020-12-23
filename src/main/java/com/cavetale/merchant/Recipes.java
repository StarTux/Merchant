package com.cavetale.merchant;

import java.util.ArrayList;
import java.util.List;

/**
 * JSONable.
 * This is stored in the recipes.json file.
 */
public final class Recipes {
    protected List<Recipe> recipes = new ArrayList<>();
    protected List<Spawn> spawns = new ArrayList<>();
    protected long recipeId = 0; // highest id used in any Recipe

    /**
     * @return true if something was changed and requires saving,
     * false otherwise.
     */
    protected boolean fix() {
        boolean result = false;
        for (Recipe recipe : recipes) {
            if (recipe.id <= 0) {
                recipe.id = ++recipeId;
                result = true;
            }
        }
        return result;
    }
}
