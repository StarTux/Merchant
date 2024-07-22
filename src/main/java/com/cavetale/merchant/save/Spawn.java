package com.cavetale.merchant.save;

import com.cavetale.core.util.Json;
import io.papermc.paper.registry.RegistryKey;
import java.io.Serializable;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import static io.papermc.paper.registry.RegistryAccess.registryAccess;

/**
 * JSONable.
 */
@Data
public final class Spawn implements Serializable {
    protected String name;
    protected String world;
    protected double x;
    protected double y;
    protected double z;
    protected float pitch;
    protected float yaw;
    protected String merchant;
    protected String displayNameJson; // Component
    protected Appearance appearance;
    protected transient Component displayNameComponent; // Cache

    @Data
    public static final class Appearance implements Serializable {
        protected EntityType entityType;
        protected String villagerProfession;
        protected String villagerType;
        protected int villagerLevel;

        public Villager.Profession parseVillagerProfession() {
            // lower case for LEGACY enums
            final NamespacedKey key = NamespacedKey.fromString(villagerProfession.toLowerCase());
            return registryAccess().getRegistry(RegistryKey.VILLAGER_PROFESSION).get(key);
        }

        public Villager.Type parseVillagerType() {
            // lower case for LEGACY enums
            final NamespacedKey key = NamespacedKey.fromString(villagerType.toLowerCase());
            return registryAccess().getRegistry(RegistryKey.VILLAGER_TYPE).get(key);
        }
    }

    public void load(Location loc) {
        world = loc.getWorld().getName();
        x = loc.getX();
        y = loc.getY();
        z = loc.getZ();
        pitch = loc.getPitch();
        yaw = loc.getYaw();
    }

    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public String simplified() {
        return
            name
            + " " + merchant
            + ";" + world
            + ":" + (int) Math.floor(x)
            + "," + (int) Math.floor(y)
            + "," + (int) Math.floor(z);
    }

    public boolean isNearby(Location loc, double max) {
        if (!world.equals(loc.getWorld().getName())) return false;
        if (Math.abs(loc.getX() - x) > max) return false;
        if (Math.abs(loc.getY() - y) > max) return false;
        if (Math.abs(loc.getZ() - z) > max) return false;
        return true;
    }

    public void setDisplayName(Component component) {
        displayNameComponent = component;
        displayNameJson = Json.serializeComponent(component);
    }

    public Component getDisplayName() {
        if (displayNameComponent == null) {
            displayNameComponent = displayNameJson != null
                ? Json.deserializeComponent(displayNameJson)
                : Component.empty();
        }
        return displayNameComponent;
    }
}
