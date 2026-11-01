package io.github.jimbozoomer.jugcraft.mixin;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A detached lantern visibly falls using native block physics; no anonymous fire/explosion. */
@Mixin(LanternBlock.class)
public abstract class OwnerFallingLanternMixin extends Block {
    protected OwnerFallingLanternMixin(Properties properties){super(properties);}
    @Inject(method="updateShape",at=@At("RETURN"),cancellable=true)
    private void jugcraft$fallLater(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos neighbor,BlockState next,RandomSource random,CallbackInfoReturnable<BlockState> cir){
        if(cir.getReturnValue().isAir()&&!state.canSurvive(level,pos)&&level.getBlockState(pos).is(state.getBlock())){ticks.scheduleTick(pos,this,1);cir.setReturnValue(state);}
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(state.canSurvive(level,pos))return;
        if(level.hasChunkAt(pos.below())&&FallingBlock.isFree(level.getBlockState(pos.below()))&&pos.getY()>level.getMinY())FallingBlockEntity.fall(level,pos,state.setValue(LanternBlock.HANGING,false));
        else level.destroyBlock(pos,true);
    }
}
