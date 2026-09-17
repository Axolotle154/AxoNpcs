package org.axostudio.axonpcs.runtime.registry

import org.axostudio.axonpcs.domain.group.NpcGroup
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class GroupRegistry {
    private val groups = ConcurrentHashMap<String, NpcGroup>()

    fun register(group: NpcGroup) {
        groups[normalize(group.name)] = group
    }

    fun unregister(name: String): NpcGroup? = groups.remove(normalize(name))

    fun get(name: String): NpcGroup? = groups[normalize(name)]

    fun getAll(): Collection<NpcGroup> = groups.values

    fun exists(name: String): Boolean = groups.containsKey(normalize(name))

    fun clear() {
        groups.clear()
    }

    private fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)
}
