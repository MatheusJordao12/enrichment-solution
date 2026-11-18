package com.enrichmentsolution.mod.common.block;

/** Implemented by machine block entities with Mekanism-style relative side configuration. */
public interface ISideConfigurableBlockEntity {
    SideConfigMode getRelativeSideMode(RelativeSide side);

    void cycleRelativeSideMode(int ordinal);
}
