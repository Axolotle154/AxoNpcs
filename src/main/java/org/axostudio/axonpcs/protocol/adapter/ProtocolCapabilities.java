package org.axostudio.axonpcs.protocol.adapter;

/**
 * Declares capabilities discovered at runtime on the current server platform.
 */
public record ProtocolCapabilities(
        boolean hasServerboundAttackPacket,
        boolean hasRelativeMovePosRot,
        boolean hasScaleAttribute,
        boolean hasMaxHealthAttribute,
        int skinCustomizationDataWatcherIndex,
        int livingHealthDataWatcherIndex,
        int armorStandClientFlagsDataWatcherIndex
) {
}
