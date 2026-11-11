package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.tower.crew.CrewShipHull;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The drop ship's hull is solid with Lithium too (Justin, 6 November 2026: "there's still no collision on the ship"; his
 * pack has Lithium). Lithium takes over an entity's move: it no longer asks the level for the other entities' boxes
 * where CrewShipCollisionMixin adds the hull, but gathers them itself (LithiumEntityCollisions.appendEntityCollisions);
 * the hull's boxes are added there as well. Only applies with Lithium installed; should Lithium rename this, the game
 * still starts and the ship is soft again (TowerGameTests.landedDropShipIsSolid tells).
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.lithium.common.entity.LithiumEntityCollisions", remap = false)
public abstract class LithiumShipHullMixin {
	@Inject(method = "appendEntityCollisions", at = @At("TAIL"), require = 0, remap = false)
	private static void jugcraft$shipHulls(List<VoxelShape> collisions, Level level, Entity mover, AABB box, CallbackInfo info) {
		List<VoxelShape> hulls = CrewShipHull.withHulls(level, mover, box, List.of());
		if (!hulls.isEmpty()) {
			try {
				collisions.addAll(hulls);
			} catch (UnsupportedOperationException fixed) {
				// a list that can't take more: nothing to be done here
			}
		}
	}
}
