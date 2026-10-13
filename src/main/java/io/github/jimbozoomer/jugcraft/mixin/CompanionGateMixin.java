package io.github.jimbozoomer.jugcraft.mixin;

import local.peepo.CompanionGates;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Livestock retain the closed-gate barrier during a companion's visibly open passage. */
@Mixin(FenceGateBlock.class)
public abstract class CompanionGateMixin extends Block {
    protected CompanionGateMixin(BlockBehaviour.Properties properties){super(properties);}
    @Inject(method="<init>",at=@At("RETURN"))
    private void jugcraft$normalDefault(CallbackInfo ci){registerDefaultState(defaultBlockState().setValue(CompanionGates.PASSAGE,false));}
    @Inject(method="createBlockStateDefinition",at=@At("TAIL"))
    private void jugcraft$passageState(StateDefinition.Builder<Block,BlockState> builder,CallbackInfo ci){builder.add(CompanionGates.PASSAGE);}
    @ModifyVariable(method="useWithoutItem",at=@At("HEAD"),argsOnly=true)
    private BlockState jugcraft$playerTakesOver(BlockState state){return state.setValue(CompanionGates.PASSAGE,false);}
    @Inject(method="getCollisionShape",at=@At("HEAD"),cancellable=true)
    private void jugcraft$keepAnimalsIn(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context,CallbackInfoReturnable<VoxelShape> cir){
        if(CompanionGates.guarded(state) && context instanceof EntityCollisionContext entity && entity.getEntity() instanceof Animal)
            cir.setReturnValue(state.setValue(FenceGateBlock.OPEN,false).getCollisionShape(level,pos,context));
    }
    // FenceGateBlock inherits the empty scheduled tick; add its narrowly scoped override.
    protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){CompanionGates.scheduled(state,level,pos);}
}
