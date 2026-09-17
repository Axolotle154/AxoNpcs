package org.axostudio.axonpcs.core.lifecycle

enum class LifecycleState {
    NOT_STARTED,
    BOOTSTRAPPING,
    RUNNING,
    STOPPING,
    STOPPED
}

class PluginLifecycle {
    @Volatile
    var state: LifecycleState = LifecycleState.NOT_STARTED
        private set

    fun markBootstrapping() {
        state = LifecycleState.BOOTSTRAPPING
    }

    fun markRunning() {
        state = LifecycleState.RUNNING
    }

    fun markStopping() {
        state = LifecycleState.STOPPING
    }

    fun markStopped() {
        state = LifecycleState.STOPPED
    }

    fun isRunning(): Boolean = state == LifecycleState.RUNNING
}
