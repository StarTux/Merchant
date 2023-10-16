package com.cavetale.merchant.save;

import com.cavetale.core.font.Unicode;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.merchant.Items;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * JSONable.
 */
@Data
public final class Recipe {
    protected long id = -1;
    protected String merchant;
    protected String inA;
    protected String inB;
    protected String out;
    protected int maxUses = -1;

    public ItemStack getInputA() {
        return Items.deserialize(inA);
    }

    public ItemStack getInputB() {
        return Items.deserialize(inB);
    }

    public ItemStack getOutput() {
        return Items.deserialize(out);
    }

    public Component toComponent() {
        return textOfChildren(text(merchant, YELLOW),
                              space(),
                              toTradeComponent());
    }

    public Component toTradeComponent() {
        ItemStack a = Items.simplify(getInputA());
        ItemStack b = Items.simplify(getInputB());
        ItemStack c = Items.simplify(getOutput());
        return textOfChildren((maxUses >= 0
                               ? text(maxUses + Unicode.MULTIPLICATION.string, LIGHT_PURPLE)
                               : text(Unicode.INFINITY.string, DARK_PURPLE)),
                              space(),
                              (a != null
                               ? ItemKinds.chatDescription(a)
                               : text("_", DARK_GRAY)),
                              text(" + ", GRAY),
                              (b != null
                               ? ItemKinds.chatDescription(b)
                               : text("_", DARK_GRAY)),
                              text(" -> ", GRAY),
                              (c != null
                               ? ItemKinds.chatDescription(c)
                               : text("_", DARK_GRAY)));
    }
}
