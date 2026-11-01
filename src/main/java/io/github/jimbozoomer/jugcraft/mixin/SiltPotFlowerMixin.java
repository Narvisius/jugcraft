package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.building.OwnerSiltPot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rooted-dirt-filled pots support flowers, while empty storage pots do not. */
@Mixin(VegetationBlock.class)
abstract class SiltPotFlowerMixin {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void jugcraft$filledPot(BlockState below, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof FlowerBlock && below.getBlock() instanceof OwnerSiltPot) cir.setReturnValue(below.getValue(OwnerSiltPot.FILLED));
    }
}
