package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The service bay's lifted cradle is solid from every side (Justin, 6 November 2026: "make the drone platform collidable.
 * Don't be able to walk through this little drone platform"). The cradle reaches out over the cells round its stand; the
 * game gathers such big shapes only from the cells straight across from a mover, never from the cells diagonally off it
 * (those it keeps for a moving piston), so a player coming at the cradle's corners walked into it. A service stand now
 * counts there too. Not required: should a game update move this, the game still starts and the corners are soft again.
 */
@Mixin(BlockCollisions.class)
public abstract class LargeShapeEdgeMixin {
	@WrapOperation(method = "computeNext", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"), require = 0)
	private boolean jugcraft$cradleCorners(BlockState state, Object block, Operation<Boolean> original) {
		return original.call(state, block) || state.getBlock() instanceof HallFixtureBlock.Stand && state.hasLargeCollisionShape();
	}
}
