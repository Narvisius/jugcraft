package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Wake native bubble updates when a grate stack or its heat source changes. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class OwnerGrateUpdatesMixin {
    @Unique private static final TagKey<Block> JUGCRAFT_GRATES=TagKey.create(Registries.BLOCK,Jugcraft.id("amends_bubble_column_passable"));
    @Inject(method="updateShape",at=@At("RETURN"))
    private void jugcraft$propagate(LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos neighbor,BlockState neighborState,RandomSource random,CallbackInfoReturnable<BlockState> cir){
        if(!(level instanceof net.minecraft.server.level.ServerLevel)||side!=Direction.UP&&side!=Direction.DOWN)return;
        var original=(BlockState)(Object)this;if(!original.is(JUGCRAFT_GRATES)&&!cir.getReturnValue().is(JUGCRAFT_GRATES))return;
        // At most sixteen grates each way. Never force loads or replace the actual grate.
        for(int distance=1;distance<=17;distance++){
            var at=pos.above(distance);if(!level.hasChunkAt(at))break;var found=level.getBlockState(at);
            if(found.is(JUGCRAFT_GRATES))continue;if(found.getBlock() instanceof BubbleColumnBlock)ticks.scheduleTick(at,found.getBlock(),1);break;
        }
        for(int distance=1;distance<=17;distance++){
            var at=pos.below(distance);if(!level.hasChunkAt(at))break;var found=side==Direction.DOWN&&distance==1?neighborState:level.getBlockState(at);
            if(found.is(JUGCRAFT_GRATES)&&found.getFluidState().is(FluidTags.WATER))continue;
            if(found.getBlock() instanceof BubbleColumnBlock||found.is(BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP)||found.is(BlockTags.ENABLES_BUBBLE_COLUMN_DRAG_DOWN))ticks.scheduleTick(at,found.getBlock(),1);
            break;
        }
    }
}
