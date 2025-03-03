package com.cavetale.merchant;

import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsCategory;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import static com.cavetale.core.util.CamelCase.toCamelCase;
import static net.kyori.adventure.text.Component.text;

@Getter
@RequiredArgsConstructor
public final class FurnitureMenu {
    private final Player player;
    private final MytemsCategory category;
    private final int cost;

    public InventoryView open() {
        final List<MerchantRecipe> recipes = new ArrayList<>();
        for (Mytems mytems : Mytems.values()) {
            if (mytems.category != category) continue;
            if (mytems == Mytems.TOILET && category == MytemsCategory.CHAIR) continue; // temporary
            final MerchantRecipe recipe = new MerchantRecipe(mytems.createItemStack(), 64);
            recipe.setIngredients(List.of(Mytems.RUBY.createItemStack(cost), Mytems.GOLDEN_COIN.createItemStack(cost)));
            recipes.add(recipe);
        }
        Merchant merchant = Bukkit.createMerchant(text(toCamelCase(" ", category) + "s"));
        merchant.setRecipes(recipes);
        return player.openMerchant(merchant, true);
    }
}
