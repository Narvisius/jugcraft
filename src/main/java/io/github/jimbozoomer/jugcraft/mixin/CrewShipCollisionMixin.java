package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShipHull;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A landed crew drop ship's hull is solid (Justin, 6 November 2026: "when I fly into the ship, I'm just flying through
 * it"): where an entity's move gathers the other entities' collision boxes, the landed ships' hull boxes are added
 * (tower/crew/CrewShipHull), on both sides. Not required: should a game update move these calls, the game still starts
 * (the ships just aren't solid) and TowerGameTests.landedDropShipIsSolid fails.
 */
@Mixin(Entity.class)
public abstract class CrewShipCollisionMixin {
	private static final String GET = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;";

	@WrapOperation(method = "collide", at = @At(value = "INVOKE", target = GET), require = 0)
	private List<VoxelShape> jugcraft$shipHulls(Level level, Entity mover, AABB area, Operation<List<VoxelShape>> original) {
		return CrewShipHull.withHulls(level, mover, area, original.call(level, mover, area));
	}

	@WrapOperation(method = "collectAllColliders", at = @At(value = "INVOKE", target = GET), require = 0)
	private static List<VoxelShape> jugcraft$shipHullsAll(Level level, Entity mover, AABB area, Operation<List<VoxelShape>> original) {
		return CrewShipHull.withHulls(level, mover, area, original.call(level, mover, area));
	}
}
