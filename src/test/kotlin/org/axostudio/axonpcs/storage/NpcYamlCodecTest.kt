package org.axostudio.axonpcs.storage

import org.axostudio.axonpcs.api.model.NPCActionTrigger
import org.axostudio.axonpcs.api.model.NPCSkin
import org.axostudio.axonpcs.api.model.NPCSkinMode
import org.axostudio.axonpcs.domain.action.ActionDefinition
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import org.axostudio.axonpcs.storage.yaml.NpcYamlCodec
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class NpcYamlCodecTest {

    @Test
    fun testRoundtripEncodeDecode(@TempDir tempDir: File) {
        val file = File(tempDir, "test_npc.yml")

        val original = NpcDefinition(
            id = "test_npc",
            type = "PLAYER",
            transform = NpcTransform("world_nether", 123.5, 64.0, -456.25, 90.0f, 15.0f),
            displayName = "<gradient:#ff0000:#00ff00>Epic NPC</gradient>",
            skin = NPCSkin(NPCSkinMode.TEXTURE, "custom_texture", "value123", "sig456"),
            glowing = "blue",
            isCollidable = true,
            isSoundEnabled = false,
            maxHealth = 100.0,
            scale = 1.5,
            viewDistance = 64.0,
            interactionCooldownSeconds = 2.0,
            isTurnToPlayer = true,
            turnToPlayerDistance = 12.0,
            behaviorType = "roam",
            behaviorRange = 5.0,
            animationType = "sneak",
            animationIntervalTicks = 60L,
            group = "lobby",
            isEnabled = true
        )

        original.appearance["pose"] = "sitting"
        original.actions.add(ActionDefinition(NPCActionTrigger.RIGHT_CLICK, "message", "Hello traveler!"))
        original.actions.add(ActionDefinition(NPCActionTrigger.LEFT_CLICK, "command", "say Ouch!"))

        // Encode to file
        NpcYamlCodec.encode(original, file)
        assertTrue(file.exists())

        // Decode from file
        val decoded = NpcYamlCodec.decode(file, "test_npc", "lobby")
        assertNotNull(decoded)

        assertEquals("test_npc", decoded!!.id)
        assertEquals("PLAYER", decoded.type)
        assertEquals("world_nether", decoded.transform.world)
        assertEquals(123.5, decoded.transform.x, 0.001)
        assertEquals(64.0, decoded.transform.y, 0.001)
        assertEquals(-456.25, decoded.transform.z, 0.001)
        assertEquals(90.0f, decoded.transform.yaw, 0.001f)
        assertEquals(15.0f, decoded.transform.pitch, 0.001f)
        assertEquals("<gradient:#ff0000:#00ff00>Epic NPC</gradient>", decoded.displayName)
        assertEquals(NPCSkinMode.TEXTURE, decoded.skin.mode())
        assertEquals("custom_texture", decoded.skin.source())
        assertEquals("value123", decoded.skin.value())
        assertEquals("sig456", decoded.skin.signature())
        assertEquals("blue", decoded.glowing)
        assertTrue(decoded.isCollidable)
        assertFalse(decoded.isSoundEnabled)
        assertEquals(100.0, decoded.maxHealth, 0.001)
        assertEquals(1.5, decoded.scale, 0.001)
        assertEquals(64.0, decoded.viewDistance, 0.001)
        assertEquals(2.0, decoded.interactionCooldownSeconds, 0.001)
        assertTrue(decoded.isTurnToPlayer)
        assertEquals(12.0, decoded.turnToPlayerDistance, 0.001)
        assertEquals("roam", decoded.behaviorType)
        assertEquals(5.0, decoded.behaviorRange, 0.001)
        assertEquals("sneak", decoded.animationType)
        assertEquals(60L, decoded.animationIntervalTicks)
        assertEquals("lobby", decoded.group)
        assertTrue(decoded.isEnabled)

        // Appearance
        assertEquals("sitting", decoded.appearance["pose"])

        // Actions
        assertEquals(2, decoded.actions.size)
        val rightClickAction = decoded.actions.find { it.trigger == NPCActionTrigger.RIGHT_CLICK }
        assertNotNull(rightClickAction)
        assertEquals("message", rightClickAction!!.type)
        assertEquals("Hello traveler!", rightClickAction.value)

        val leftClickAction = decoded.actions.find { it.trigger == NPCActionTrigger.LEFT_CLICK }
        assertNotNull(leftClickAction)
        assertEquals("command", leftClickAction!!.type)
        assertEquals("say Ouch!", leftClickAction.value)
    }
}
