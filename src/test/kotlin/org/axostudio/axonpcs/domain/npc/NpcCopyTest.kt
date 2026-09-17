package org.axostudio.axonpcs.domain.npc

import org.axostudio.axonpcs.api.model.NPCActionTrigger
import org.axostudio.axonpcs.api.model.NPCSkin
import org.axostudio.axonpcs.api.model.NPCSkinMode
import org.axostudio.axonpcs.domain.action.ActionDefinition
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NpcCopyTest {

    @Test
    fun `copies all properties accurately to new npc`() {
        val original = NpcDefinition(
            id = "original_guard",
            type = "PLAYER",
            transform = NpcTransform("world", 10.0, 65.0, -20.0, 45f, 10f),
            displayName = "<gold>Guard</gold>",
            skin = NPCSkin(NPCSkinMode.TEXTURE, "custom", "skin_val", "skin_sig"),
            glowing = "gold",
            isCollidable = true,
            isSoundEnabled = false,
            maxHealth = 40.0,
            scale = 1.25,
            viewDistance = 32.0,
            interactionCooldownSeconds = 2.5,
            isTurnToPlayer = true,
            turnToPlayerDistance = 14.0,
            behaviorType = "stationary",
            behaviorRange = 6.0,
            animationType = "wave",
            animationIntervalTicks = 80L,
            group = "guards",
            isEnabled = true
        ).apply {
            appearance["pose"] = "standing"
            actions.add(ActionDefinition(NPCActionTrigger.RIGHT_CLICK, "message", "Halt!"))
        }

        val newTransform = NpcTransform("world_nether", 50.0, 70.0, 100.0, 180f, 0f)
        val copy = original.copy("cloned_guard", newTransform)

        assertEquals("cloned_guard", copy.id)
        assertNotEquals(original.uniqueId, copy.uniqueId)
        assertEquals("PLAYER", copy.type)
        assertEquals("world_nether", copy.transform.world)
        assertEquals(50.0, copy.transform.x)
        assertEquals("<gold>Guard</gold>", copy.displayName)
        assertEquals("skin_val", copy.skin.value())
        assertEquals("gold", copy.glowing)
        assertTrue(copy.isCollidable)
        assertFalse(copy.isSoundEnabled)
        assertEquals(40.0, copy.maxHealth)
        assertEquals(1.25, copy.scale)
        assertEquals("guards", copy.group)
        assertEquals("standing", copy.appearance["pose"])
        assertEquals(1, copy.actions.size)
        assertEquals("Halt!", copy.actions[0].value)

        // Ensure mutating copy doesn't mutate original
        copy.actions.add(ActionDefinition(NPCActionTrigger.LEFT_CLICK, "command", "attack"))
        copy.appearance["pose"] = "sitting"
        assertEquals(1, original.actions.size)
        assertEquals("standing", original.appearance["pose"])
    }
}
