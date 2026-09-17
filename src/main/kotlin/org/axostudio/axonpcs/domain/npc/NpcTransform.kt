package org.axostudio.axonpcs.domain.npc

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World

data class NpcTransform(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0.0f,
    val pitch: Float = 0.0f
) {
    fun toBukkitLocation(): Location? {
        val bukkitWorld: World? = Bukkit.getWorld(world)
        return bukkitWorld?.let { Location(it, x, y, z, yaw, pitch) }
    }

    fun withPosition(x: Double, y: Double, z: Double): NpcTransform =
        copy(x = x, y = y, z = z)

    fun withRotation(yaw: Float, pitch: Float): NpcTransform =
        copy(yaw = yaw, pitch = pitch)

    fun distanceSquared(loc: Location): Double {
        if (loc.world == null || !loc.world.name.equals(world, ignoreCase = true)) {
            return Double.MAX_VALUE
        }
        val dx = loc.x - x
        val dy = loc.y - y
        val dz = loc.z - z
        return dx * dx + dy * dy + dz * dz
    }

    companion object {
        fun fromBukkit(loc: Location): NpcTransform =
            NpcTransform(
                world = loc.world?.name ?: "world",
                x = loc.x,
                y = loc.y,
                z = loc.z,
                yaw = loc.yaw,
                pitch = loc.pitch
            )
    }
}
