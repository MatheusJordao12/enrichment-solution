package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigMode;

/**
 * Implemented by machine menus that expose Mekanism-style relative side configuration to their screens.
 */
public interface ISideConfigurableMenu {

    SideConfigMode getRelativeSideMode(RelativeSide side);
}
