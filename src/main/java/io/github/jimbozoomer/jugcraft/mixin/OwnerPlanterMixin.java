package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Planters are permanent irrigated soil, including crops which override vegetation support. */
@Mixin({VegetationBlock.class,CropBlock.class,StemBlock.class})
abstract class OwnerPlanterMixin {
    @Inject(method="mayPlaceOn",at=@At("HEAD"),cancellable=true)
    private void jugcraft$planter(BlockState below,BlockGetter level,BlockPos pos,CallbackInfoReturnable<Boolean> result){
        if(below.getBlock() instanceof OwnerSupplementBlock b&&b.function.equals("planter"))result.setReturnValue(true);
    }
}
