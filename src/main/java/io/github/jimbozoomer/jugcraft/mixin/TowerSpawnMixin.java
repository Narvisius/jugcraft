package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.tower.TowerRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nothing spawns in or on a Drone Tower of itself (Justin, 5 November 2026: "make it so mobs do not spawn on/in the
 * tower"): the world's spawning (every mob, monsters and animals, the mod's and the pack's, whatever its own rules say)
 * finds no place there ({@link TowerRegistry#inTower}). What the tower or a player puts there (its crew, spawn eggs,
 * pets) is not natural spawning and still comes.
 */
@Mixin(NaturalSpawner.class)
public abstract class TowerSpawnMixin {
	@Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
	private static void jugcraft$noSpawnInTowers(ServerLevel level, Mob mob, double distance, CallbackInfoReturnable<Boolean> result) {
		if (TowerRegistry.get(level).inTower(level, mob.blockPosition())) {
			result.setReturnValue(false);
		}
	}
}
