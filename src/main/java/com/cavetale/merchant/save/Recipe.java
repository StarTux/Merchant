package com.cavetale.merchant.save;

import com.cavetale.core.font.Unicode;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.merchant.Items;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
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

    public static ItemStack ifMytems(ItemStack in) {
        if (in == null) return null;
        Mytems mytems = Mytems.forItem(in);
        if (mytems == null) return in;
        String serialized = mytems.serializeItem(in);
        ItemStack res = Mytems.deserializeItem(serialized);
        return res;
    }

    public MerchantRecipe toMerchantRecipe() {
        final ItemStack a = ifMytems(Items.deserialize(getInA()));
        final ItemStack b = ifMytems(Items.deserialize(getInB()));
        final ItemStack c = ifMytems(Items.deserialize(getOut()));
        List<ItemStack> inputList = new ArrayList<>(2);
        inputList.add(a);
        if (b != null) inputList.add(b);
        MerchantRecipe result = new MerchantRecipe(c, maxUses >= 0 ? maxUses : 999);
        result.setUses(0);
        result.setIngredients(inputList);
        return result;
    }
}
