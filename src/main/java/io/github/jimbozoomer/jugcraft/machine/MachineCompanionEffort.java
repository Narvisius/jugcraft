package io.github.jimbozoomer.jugcraft.machine;

import com.mojang.serialization.Codec;

/** Reserve JE per productive tick per +25% contribution, independent of electrical upgrades. */
public final class MachineCompanionEffort {
    public static final int MAX_PER_QUARTER = 16;
    /** Zero selects the machine default; a positive recipe value overrides it. */
    public static final Codec<Integer> CODEC = Codec.intRange(0, MAX_PER_QUARTER);

    private MachineCompanionEffort() {}

    public static int perQuarter(MachineKind machine, int recipeOverride) {
        if (recipeOverride > 0) return Math.min(MAX_PER_QUARTER, recipeOverride);
        // Unpowered hot-work stations still involve physical effort.
        if (machine == MachineKind.COKE_OVEN) return 4;
        if (machine == MachineKind.STEEL_FOUNDRY) return 6;
        int base = machine.usePerTick;
        if (base <= 12) return 2;
        if (base <= 24) return 4;
        if (base <= 64) return 6;
        if (base <= 128) return 10;
        return MAX_PER_QUARTER;
    }
}
