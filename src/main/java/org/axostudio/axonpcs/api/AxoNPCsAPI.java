package org.axostudio.axonpcs.api;

import org.axostudio.axonpcs.api.model.AxoNPC;
import org.axostudio.axonpcs.api.service.AxoNPCService;

import java.util.Collection;

/**
 * Legacy API interface for AxoNPCs, extending {@link AxoNPCService} for backward compatibility.
 */
public interface AxoNPCsAPI extends AxoNPCService {

    /**
     * Legacy alias for {@link #getAllNPCs()}.
     *
     * @return Collection of all registered NPCs.
     */
    default Collection<AxoNPC> getNPCs() {
        return getAllNPCs();
    }
}
