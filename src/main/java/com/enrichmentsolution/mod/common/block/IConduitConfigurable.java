package com.enrichmentsolution.mod.common.block;

/** Implemented by cable/pipe block entities that expose per-side push/pull configuration. */
public interface IConduitConfigurable {
    ConduitSideConfig getConduitConfig();

    void onConduitConfigChanged();
}
