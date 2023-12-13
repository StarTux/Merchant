package com.cavetale.merchant.save;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.bukkit.inventory.MerchantRecipe;

/**
 * JSONable.
 * Represents one file in the merchants folder.
 */
@Data
public final class MerchantFile {
    protected String name;
    protected boolean persistent;
    protected List<Recipe> recipes;

    public void fix() {
        long nextId = 0;
        for (Recipe recipe : recipes) {
            recipe.merchant = name;
            recipe.id = nextId++;
        }
    }

    public MerchantFile() { }

    public MerchantFile(final String name) {
        this.name = name;
        this.recipes = new ArrayList<>();
    }

    public List<MerchantRecipe> toMerchantRecipeList() {
        List<MerchantRecipe> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            result.add(recipe.toMerchantRecipe());
        }
        return result;
    }
}
