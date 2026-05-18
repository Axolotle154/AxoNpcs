package org.axostudio.axonpcs.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class NPCPosition {
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public NPCPosition(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static NPCPosition from(Location location) {
        String worldName = location.getWorld() == null ? "world" : location.getWorld().getName();
        return new NPCPosition(worldName, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public String world() {
        return world;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public Location toLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    public NPCPosition withRotation(float yaw, float pitch) {
        return new NPCPosition(world, x, y, z, yaw, pitch);
    }
}
