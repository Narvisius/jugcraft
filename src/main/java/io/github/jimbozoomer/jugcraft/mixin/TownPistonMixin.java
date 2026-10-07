package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.town.TownProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pistons cannot push or pull the town's protected blocks. */
@Mixin(PistonBaseBlock.class)
public class TownPistonMixin {
	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private static void jugcraft$keepTown(BlockState state, Level level, BlockPos pos, Direction direction, boolean allowDestroy,
			Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
		if (TownProtection.shieldsBlock(level, pos)) {
			cir.setReturnValue(false);
		}
	}
}
