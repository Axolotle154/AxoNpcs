package org.axostudio.axonpcs.domain.npc

import org.axostudio.axonpcs.api.model.*
import org.axostudio.axonpcs.domain.action.ActionDefinition
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.util.UUID

class NpcDefinition(
    @JvmField var id: String,
    @JvmField var uuid: UUID = UUID.nameUUIDFromBytes("AxoNPCs:$id".toByteArray(Charsets.UTF_8)),
    @JvmField var type: String = "PLAYER",
    @JvmField var transform: NpcTransform,
    @JvmField var displayName: String? = null,
    @JvmField var skin: NPCSkin = NPCSkin.none(),
    @JvmField var glowing: String = "off",
    @JvmField var isCollidable: Boolean = false,
    @JvmField var isSoundEnabled: Boolean = true,
    @JvmField var maxHealth: Double = 20.0,
    @JvmField var scale: Double = 1.0,
    @JvmField var viewDistance: Double = 48.0,
    @JvmField var interactionCooldownSeconds: Double = 1.5,
    @JvmField var isTurnToPlayer: Boolean = true,
    @JvmField var turnToPlayerDistance: Double = 16.0,
    @JvmField var behaviorType: String = "stationary",
    @JvmField var behaviorRange: Double = 8.0,
    @JvmField var animationType: String = "none",
    @JvmField var animationIntervalTicks: Long = 40L,
    @JvmField val equipment: MutableMap<NPCEquipmentSlot, ItemStack> = mutableMapOf(),
    @JvmField val appearance: MutableMap<String, String> = mutableMapOf(),
    @JvmField val actions: MutableList<ActionDefinition> = mutableListOf(),
    @JvmField var group: String? = null,
    @JvmField var isEnabled: Boolean = true
) : AxoNPC {

    override fun getId(): String = id

    override fun getUniqueId(): UUID = uuid

    override fun getType(): String = type

    override fun getLocation(): Location = transform.toBukkitLocation()
        ?: Location(null, transform.x, transform.y, transform.z, transform.yaw, transform.pitch)

    override fun getDisplayName(): String? = displayName

    override fun getSkin(): NPCSkin = skin

    override fun getGlowing(): String = glowing

    override fun isCollidable(): Boolean = isCollidable

    override fun isSoundEnabled(): Boolean = isSoundEnabled

    override fun getMaxHealth(): Double = maxHealth

    override fun getScale(): Double = scale

    override fun getViewDistance(): Double = viewDistance

    override fun getInteractionCooldownSeconds(): Double = interactionCooldownSeconds

    override fun isTurnToPlayer(): Boolean = isTurnToPlayer

    override fun getTurnToPlayerDistance(): Double = turnToPlayerDistance

    override fun getEquipment(): Map<NPCEquipmentSlot, ItemStack> = equipment

    override fun getAppearance(): Map<String, String> = appearance

    override fun getActions(trigger: NPCActionTrigger): List<NPCAction> {
        return actions.filter { it.trigger == trigger || it.trigger == NPCActionTrigger.ANY }
            .map { it.toApi() }
    }

    override fun getGroup(): String? = group
    fun copy(newId: String, newTransform: NpcTransform = this.transform): NpcDefinition {
        val clone = NpcDefinition(
            id = newId.lowercase(java.util.Locale.ROOT),
            type = this.type,
            transform = newTransform,
            displayName = this.displayName,
            skin = this.skin,
            glowing = this.glowing,
            isCollidable = this.isCollidable,
            isSoundEnabled = this.isSoundEnabled,
            maxHealth = this.maxHealth,
            scale = this.scale,
            viewDistance = this.viewDistance,
            interactionCooldownSeconds = this.interactionCooldownSeconds,
            isTurnToPlayer = this.isTurnToPlayer,
            turnToPlayerDistance = this.turnToPlayerDistance,
            behaviorType = this.behaviorType,
            behaviorRange = this.behaviorRange,
            animationType = this.animationType,
            animationIntervalTicks = this.animationIntervalTicks,
            group = this.group,
            isEnabled = this.isEnabled
        )
        this.equipment.forEach { (slot, item) -> clone.equipment[slot] = item.clone() }
        this.appearance.forEach { (k, v) -> clone.appearance[k] = v }
        this.actions.forEach { action -> clone.actions.add(ActionDefinition(action.trigger, action.type, action.value)) }
        return clone
    }
}
