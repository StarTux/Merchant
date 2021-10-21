package com.cavetale.merchant.save;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * JSONable.
 * This is stored in the recipes.json file.
 */
@Data
public final class Recipes {
    protected List<Recipe> recipes = new ArrayList<>();
    protected List<Spawn> spawns = new ArrayList<>();
}
