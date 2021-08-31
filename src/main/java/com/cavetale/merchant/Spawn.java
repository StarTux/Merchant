package com.cavetale.merchant;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * JSONable.
 */
@Data
public final class Spawn {
    protected String world;
    protected double x;
    protected double y;
    protected double z;
    protected float pitch;
    protected float yaw;
    protected String merchant;

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
            merchant
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
}
