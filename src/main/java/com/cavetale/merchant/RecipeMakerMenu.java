package com.cavetale.merchant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

@RequiredArgsConstructor
public final class RecipeMakerMenu implements InventoryHolder {
    final String merchant;
    @Getter Inventory inventory;
}
