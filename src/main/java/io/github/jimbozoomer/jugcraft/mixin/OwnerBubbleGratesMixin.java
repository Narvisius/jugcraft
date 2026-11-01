package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bubble flow passes through supplied waterlogged grates without erasing those blocks. */
@Mixin(BubbleColumnBlock.class)
public abstract class OwnerBubbleGratesMixin {
    @Unique private static final TagKey<Block> JUGCRAFT_GRATES=TagKey.create(Registries.BLOCK,Jugcraft.id("amends_bubble_column_passable"));
    @Unique private static boolean jugcraft$grate(BlockState state){return state.is(JUGCRAFT_GRATES)&&state.getFluidState().is(FluidTags.WATER)&&state.getFluidState().isSource();}
    @Inject(method="canOccupy",at=@At("RETURN"),cancellable=true)
    private static void jugcraft$pass(Block type,BlockState state,CallbackInfoReturnable<Boolean> cir){if(jugcraft$grate(state))cir.setReturnValue(true);}
    @Redirect(method="updateColumn(Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean jugcraft$preserve(LevelAccessor level,BlockPos pos,BlockState state,int flags){return jugcraft$grate(level.getBlockState(pos))||level.setBlock(pos,state,flags);}
    @ModifyVariable(method="updateColumn(Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static BlockState jugcraft$fluidOccupant(BlockState occupant){return jugcraft$grate(occupant)?Blocks.WATER.defaultBlockState():occupant;}
    @ModifyVariable(method="updateColumn(Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private static BlockState jugcraft$below(BlockState below,Block type,LevelAccessor level,BlockPos pos,BlockState occupant,BlockState originalBelow){
        if(!jugcraft$grate(below))return below;
        for(int i=2;i<=17;i++){var at=pos.below(i);if(!level.hasChunkAt(at))break;var found=level.getBlockState(at);if(!jugcraft$grate(found))return found;}
        return Blocks.STONE.defaultBlockState();
    }
    @Inject(method="canSurvive",at=@At("RETURN"),cancellable=true)
    private void jugcraft$support(BlockState state,LevelReader level,BlockPos pos,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValueZ()||!jugcraft$grate(level.getBlockState(pos.below())))return;
        for(int i=2;i<=17;i++){var at=pos.below(i);if(!level.hasChunkAt(at))return;var found=level.getBlockState(at);if(!jugcraft$grate(found)){cir.setReturnValue(found.is(state.getBlock())||found.is(BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP)||found.is(BlockTags.ENABLES_BUBBLE_COLUMN_DRAG_DOWN));return;}}
    }
}
