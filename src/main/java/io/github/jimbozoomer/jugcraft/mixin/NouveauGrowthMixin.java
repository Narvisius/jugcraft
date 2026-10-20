package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Only natural random-tick growth produces Source; direct bonemeal does not. */
@Mixin({CropBlock.class,SweetBerryBushBlock.class,SaplingBlock.class})
public abstract class NouveauGrowthMixin {
    @Inject(method="randomTick",at=@At("RETURN"))
    private void jugcraft$naturalGrowth(BlockState previous,ServerLevel level,BlockPos pos,RandomSource random,CallbackInfo ci){NouveauSource.naturalGrowth(level,pos,previous);}
}
