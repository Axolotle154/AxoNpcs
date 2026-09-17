package org.axostudio.axonpcs.domain.action

import org.axostudio.axonpcs.api.model.NPCAction
import org.axostudio.axonpcs.api.model.NPCActionTrigger

data class ActionDefinition(
    val trigger: NPCActionTrigger,
    val type: String,
    val value: String
) {
    fun toApi(): NPCAction = NPCAction(type, value)

    companion object {
        fun fromApi(trigger: NPCActionTrigger, action: NPCAction): ActionDefinition =
            ActionDefinition(trigger, action.type(), action.value())
    }
}
