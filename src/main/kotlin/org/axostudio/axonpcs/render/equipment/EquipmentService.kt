package org.axostudio.axonpcs.render.equipment

import org.axostudio.axonpcs.api.model.NPCEquipmentSlot
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class EquipmentService {

    fun equip(npc: NpcDefinition, slot: NPCEquipmentSlot, item: ItemStack?) {
        if (item == null || item.type == Material.AIR) {
            npc.equipment.remove(slot)
        } else {
            npc.equipment[slot] = item.clone()
        }
    }

    fun autoEquip(npc: NpcDefinition, player: Player) {
        val inv = player.inventory
        equip(npc, NPCEquipmentSlot.HELMET, inv.helmet)
        equip(npc, NPCEquipmentSlot.CHESTPLATE, inv.chestplate)
        equip(npc, NPCEquipmentSlot.LEGGINGS, inv.leggings)
        equip(npc, NPCEquipmentSlot.BOOTS, inv.boots)
        equip(npc, NPCEquipmentSlot.MAIN_HAND, inv.itemInMainHand)
        equip(npc, NPCEquipmentSlot.OFF_HAND, inv.itemInOffHand)
    }

    fun clear(npc: NpcDefinition) {
        npc.equipment.clear()
    }
}
