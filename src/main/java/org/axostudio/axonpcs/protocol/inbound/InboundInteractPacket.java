package org.axostudio.axonpcs.protocol.inbound;

import org.axostudio.axonpcs.api.model.NPCActionTrigger;

public record InboundInteractPacket(int entityId, NPCActionTrigger trigger) {
}
