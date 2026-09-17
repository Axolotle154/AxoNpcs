package org.axostudio.axonpcs.runtime.service

import org.axostudio.axonpcs.api.event.group.AxoNPCGroupCreateEvent
import org.axostudio.axonpcs.api.event.group.AxoNPCGroupDeleteEvent
import org.axostudio.axonpcs.domain.group.NpcGroup
import org.axostudio.axonpcs.runtime.registry.GroupRegistry
import org.axostudio.axonpcs.storage.repository.GroupRepository
import org.bukkit.Bukkit

class GroupService(
    private val groupRegistry: GroupRegistry,
    private val groupRepository: GroupRepository
) {

    fun loadAll() {
        groupRegistry.clear()
        val groups = groupRepository.listGroups()
        for (g in groups) {
            groupRegistry.register(g)
        }
    }

    fun createGroup(name: String): Boolean {
        val event = AxoNPCGroupCreateEvent(name)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return false

        val group = NpcGroup(name)
        groupRegistry.register(group)
        return groupRepository.createGroup(name)
    }

    fun deleteGroup(name: String): Boolean {
        val event = AxoNPCGroupDeleteEvent(name)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return false

        groupRegistry.unregister(name)
        return groupRepository.deleteGroup(name)
    }

    fun getGroups(): List<NpcGroup> = groupRegistry.getAll().toList()
}
