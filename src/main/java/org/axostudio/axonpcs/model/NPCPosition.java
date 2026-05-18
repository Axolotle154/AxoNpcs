package org.axostudio.axonpcs.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record NPCPosition(String world, double x, double y, double z, float yaw, float pitch) {
    public static NPCPosition from(Location location) {
        String worldName = location.getWorld() == null ? "world" : location.getWorld().getName();
        return new NPCPosition(worldName, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public Location toLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    public NPCPosition withRotation(float yaw, float pitch) {
        return new NPCPosition(world, x, y, z, yaw, pitch);
    }
}
