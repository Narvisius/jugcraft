package io.github.jimbozoomer.jugcraft.raiders;

import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import net.minecraft.world.entity.Entity;

/** Player-directed raids are deferred. Even retaliation and splash must obey this policy. */
public final class RaiderTargets {
    private RaiderTargets() {}
    public static boolean allowed(Entity target) {
        return target instanceof Townsfolk guard && guard.isGuard() && guard.isAlive();
    }
}
