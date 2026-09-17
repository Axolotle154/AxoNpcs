package org.axostudio.axonpcs.core.module

interface AxoModule {
    val name: String

    fun onEnable()

    fun onDisable()

    fun onReload() {}
}
