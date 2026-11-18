package com.enrichmentsolution.mod.common.block;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;

/** Per-direction push/pull/none configuration for cables and pipes. */
public class ConduitSideConfig {

    public enum Mode {
        AUTO("auto"), PUSH("push"), PULL("pull"), NONE("none");

        private final String key;

        Mode(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Mode previous() {
            return values()[(ordinal() + values().length - 1) % values().length];
        }
    }

    private final EnumMap<Direction, Mode> modes = new EnumMap<>(Direction.class);

    public Mode get(Direction dir) {
        return modes.getOrDefault(dir, Mode.AUTO);
    }

    public void set(Direction dir, Mode mode) {
        modes.put(dir, mode);
    }

    public boolean canPush(Direction dir) {
        Mode m = get(dir);
        return m == Mode.AUTO || m == Mode.PUSH;
    }

    public boolean canPull(Direction dir) {
        Mode m = get(dir);
        return m == Mode.AUTO || m == Mode.PULL;
    }

    public void save(CompoundTag tag) {
        int[] arr = new int[6];
        for (Direction d : Direction.values()) {
            arr[d.get3DDataValue()] = get(d).ordinal();
        }
        tag.putIntArray("ConduitSides", arr);
    }

    public void load(CompoundTag tag) {
        modes.clear();
        int[] arr = tag.getIntArray("ConduitSides");
        if (arr.length != 6) {
            return;
        }
        for (Direction d : Direction.values()) {
            int o = arr[d.get3DDataValue()];
            if (o >= 0 && o < Mode.values().length) {
                modes.put(d, Mode.values()[o]);
            }
        }
    }
}
