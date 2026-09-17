package org.axostudio.axonpcs.domain.npc

import net.kyori.adventure.text.Component
import org.axostudio.axonpcs.api.model.NPCEquipmentSlot
import org.bukkit.inventory.ItemStack

sealed interface NpcComponent {
    data class Hologram(
        val lines: List<Component>
    ) : NpcComponent

    data class Equipment(
        val items: Map<NPCEquipmentSlot, ItemStack>
    ) : NpcComponent

    data class Appearance(
        val pose: String? = null,
        val isCrouching: Boolean = false,
        val isSprinting: Boolean = false,
        val isSwimming: Boolean = false,
        val isGliding: Boolean = false,
        val isOnFire: Boolean = false,
        val isInvisible: Boolean = false
    ) : NpcComponent
}
