package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerCauldronHeat;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayeredCauldronBlock.class)
public abstract class OwnerCauldronHeatMixin extends Block {
    protected OwnerCauldronHeatMixin(Properties properties){super(properties);}
    @Inject(method="entityInside",at=@At("TAIL"))
    private void jugcraft$boil(BlockState state,Level level,BlockPos pos,Entity entity,InsideBlockEffectApplier effects,boolean pastEdges,CallbackInfo ci){if(state.is(Blocks.WATER_CAULDRON))OwnerCauldronHeat.inside(state,level,pos,entity);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){super.animateTick(state,level,pos,random);if(state.is(Blocks.WATER_CAULDRON))OwnerCauldronHeat.animate(state,level,pos,random);}
}
