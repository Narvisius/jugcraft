package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The service bay's lifted cradle is solid at its corners with Lithium too (Justin's pack has it). Lithium sweeps for
 * collisions its own way (its ChunkAwareBlockCollisionSweeper, not the game's BlockCollisions that LargeShapeEdgeMixin
 * opens up), with the same rule: a big shape counts from the cells straight across, never from the cells diagonally off
 * the mover (those it keeps for a moving piston). A service stand counts there too. Only applies with Lithium installed;
 * should Lithium rename this, the game still starts and the cradle's corners are soft again (the game test tells).
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper", remap = false)
public abstract class LithiumCradleEdgeMixin {
	@Inject(method = "canInteractWithBlock", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
	private static void jugcraft$cradleCorners(BlockState state, int edgesHit, CallbackInfoReturnable<Boolean> result) {
		if (edgesHit == 2 && state.getBlock() instanceof HallFixtureBlock.Stand && state.hasLargeCollisionShape()) {
			result.setReturnValue(true);
		}
	}
}
