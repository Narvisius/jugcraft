package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.town.TownProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fire in the town's protected area goes out on its first tick, before it burns or spreads. */
@Mixin(FireBlock.class)
public class TownFireBlockMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void jugcraft$keepTown(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (TownProtection.shieldsBlock(level, pos)) {
			level.removeBlock(pos, false);
			ci.cancel();
		}
	}
}
