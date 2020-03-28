package com.cavetale.merchant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class Items {
    private Items() { }

    public static String serialize(ItemStack item) {
        if (item == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
            boos.writeObject(item);
            baos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException ioe) {
            System.err.println("item=" + item);
            ioe.printStackTrace();
            return null;
        }
    }

    public static ItemStack deserialize(String in) {
        if (in == null) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(in);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);
            ItemStack item = (ItemStack) bois.readObject();
            bais.close();
            return item;
        } catch (IOException ioe) {
            System.err.println("in=" + in);
            ioe.printStackTrace();
            return null;
        } catch (ClassNotFoundException cnfe) {
            System.err.println("in=" + in);
            cnfe.printStackTrace();
            return null;
        }
    }

    public static String toString(ItemStack item) {
        if (item == null) return "";
        String name = Stream.of(item.getType().name().split("_"))
            .map(s -> s.substring(0, 1) + s.substring(1).toLowerCase())
            .collect(Collectors.joining(""));
        return item.getAmount() + "x" + name;
    }

    public static ItemStack simplify(ItemStack item) {
        if (item == null) return null;
        if (item.getAmount() == 0) return null;
        if (item.getType() == Material.AIR) return null;
        return item;
    }

    public static String toString(Recipe recipe) {
        return "'" + recipe.merchant + "' "
            + toString(deserialize(recipe.inA))
            + (recipe.inB != null
               ? " + " + toString(deserialize(recipe.inB))
               : "")
            + " => " + toString(deserialize(recipe.out));
    }
}
