package org.axostudio.axonpcs.core.bootstrap

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class ServiceRegistry {
    private val services = ConcurrentHashMap<KClass<*>, Any>()

    fun <T : Any> register(clazz: KClass<T>, service: T) {
        services[clazz] = service
    }

    inline fun <reified T : Any> register(service: T) {
        register(T::class, service)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: KClass<T>): T {
        val instance = services[clazz]
            ?: throw IllegalStateException("Service not registered: ${clazz.qualifiedName}")
        return instance as T
    }

    inline fun <reified T : Any> get(): T = get(T::class)

    fun clear() {
        services.clear()
    }
}
