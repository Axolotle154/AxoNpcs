package org.axostudio.axonpcs.command.npc

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.axostudio.axonpcs.api.model.AxoNPC
import org.axostudio.axonpcs.api.model.NPCActionTrigger
import org.axostudio.axonpcs.config.ConfigManager
import org.axostudio.axonpcs.domain.action.ActionDefinition
import org.axostudio.axonpcs.domain.npc.NpcDefinition
import org.axostudio.axonpcs.domain.npc.NpcTransform
import org.axostudio.axonpcs.platform.scheduler.SchedulerAdapter
import org.axostudio.axonpcs.render.equipment.EquipmentService
import org.axostudio.axonpcs.render.skin.SkinService
import org.axostudio.axonpcs.runtime.service.GroupService
import org.axostudio.axonpcs.runtime.service.NpcService
import org.axostudio.axonpcs.tracking.viewer.VisibilityController
import org.bukkit.command.CommandSender
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.assertContains
import kotlin.test.assertFalse

class NpcCommandSuggestionsTest {
    private val npc = NpcDefinition(
        id = "guard",
        transform = NpcTransform("world", 0.0, 64.0, 0.0, 0f, 0f),
        group = "spawn"
    ).apply {
        actions += ActionDefinition(NPCActionTrigger.RIGHT_CLICK, "message", "Hello")
    }

    private val npcService: NpcService = mock()
    private val groupService: GroupService = mock()
    private val sender: CommandSender = mock()
    private val source: CommandSourceStack = mock()
    private lateinit var command: NpcCommandExecutor

    @BeforeEach
    fun setUp() {
        whenever(source.sender).thenReturn(sender)
        whenever(sender.hasPermission(any<String>())).thenReturn(true)
        whenever(npcService.allNPCs).thenReturn(listOf<AxoNPC>(npc))
        whenever(npcService.getNPC("guard")).thenReturn(Optional.of(npc))
        whenever(groupService.getGroups()).thenReturn(emptyList())

        command = NpcCommandExecutor(
            mock<ConfigManager>(),
            npcService,
            mock<SkinService>(),
            mock<EquipmentService>(),
            groupService,
            mock<VisibilityController>(),
            mock<SchedulerAdapter>()
        )
    }

    @Test
    fun `suggests values for nested command arguments`() {
        assertContains(command.suggest(source, arrayOf("type", "guard", "")), "PLAYER")
        assertContains(command.suggest(source, arrayOf("equip", "guard", "main_hand", "dia")), "diamond_sword")
        assertContains(command.suggest(source, arrayOf("action", "guard", "RIGHT_CLICK", "")), "add")
        assertContains(command.suggest(source, arrayOf("action", "guard", "RIGHT_CLICK", "remove", "")), "0")
        assertContains(command.suggest(source, arrayOf("group", "guard", "")), "spawn")
    }

    @Test
    fun `uses argument-specific suggestions instead of npc ids everywhere`() {
        val nearby = command.suggest(source, arrayOf("nearby", ""))
        assertContains(nearby, "16")
        assertFalse("guard" in nearby)
    }

    @Test
    fun `suggests source npc and target id and location modes for copy and clone`() {
        assertContains(command.suggest(source, arrayOf("copy", "")), "guard")
        assertContains(command.suggest(source, arrayOf("clone", "")), "guard")
        assertContains(command.suggest(source, arrayOf("copy", "guard", "")), "guard_copy")
        assertContains(command.suggest(source, arrayOf("copy", "guard", "new_guard", "")), "here")
        assertContains(command.suggest(source, arrayOf("copy", "guard", "new_guard", "")), "same")
    }
}
