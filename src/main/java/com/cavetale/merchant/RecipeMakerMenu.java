package com.cavetale.merchant;

import com.cavetale.merchant.save.MerchantFile;
import com.cavetale.merchant.save.Recipe;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

@RequiredArgsConstructor
public final class RecipeMakerMenu implements InventoryHolder {
    final String merchant;
    protected MerchantFile merchantFile;
    protected Recipe recipe; // edit
    @Getter protected Inventory inventory;
    protected int maxUses = -1;
}
