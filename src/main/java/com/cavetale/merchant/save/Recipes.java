package com.cavetale.merchant.save;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * JSONable.
 * This is stored in the recipes.json file.
 * Legacy file!  Only kept for conversion purposes.  Recipes are now
 * stored in MerchantFile.
 */
@Data
public final class Recipes {
    protected List<Recipe> recipes = new ArrayList<>();
    protected List<Spawn> spawns = new ArrayList<>();
}
