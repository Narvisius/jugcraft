package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;

/** Move the actual pad into its water cell while a small decoration occupies its top. */
final class OwnerLilySupport {
    private OwnerLilySupport(){}
    static InteractionResult place(Player player,Level level,InteractionHand hand,BlockHitResult hit){
        var top=hit.getBlockPos();var stack=player.getItemInHand(hand);
        if(hit.getDirection()!=Direction.UP||!level.getBlockState(top).is(Blocks.LILY_PAD)||!(stack.getItem() instanceof BlockItem item))return InteractionResult.PASS;
        var b=item.getBlock();if(!(b instanceof CandleBlock||b instanceof WoolCarpetBlock||b instanceof FlowerPotBlock||b instanceof LanternBlock))return InteractionResult.PASS;
        var bottom=top.below();if(!level.hasChunkAt(bottom)||!level.getBlockState(bottom).is(Blocks.WATER)||!level.getFluidState(bottom).isSource())return InteractionResult.PASS;
        if(!Menagerie.mayChange(level,player,top)||!Menagerie.mayChange(level,player,bottom))return InteractionResult.FAIL;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        var water=level.getBlockState(bottom);
        var pad=level.getBlockState(top);
        if(!level.setBlock(bottom,OwnerAmendments.BLOCKS.get("water_lily_pad").defaultBlockState(),2))return InteractionResult.FAIL;
        level.setBlock(top,Blocks.AIR.defaultBlockState(),2);
        var result=item.place(new BlockPlaceContext(new UseOnContext(player,hand,hit)));
        if(!result.consumesAction()){
            level.setBlock(top,pad,2);level.setBlock(bottom,water,3);return result;
        }
        level.updateNeighborsAt(top,level.getBlockState(top).getBlock());level.updateNeighborsAt(bottom,OwnerAmendments.BLOCKS.get("water_lily_pad"));return result;
    }
    static void restore(ServerLevel level,BlockPos pos){
        if(!level.getBlockState(pos.above()).isAir())return;
        level.setBlock(pos,Blocks.WATER.defaultBlockState(),2);level.setBlock(pos.above(),Blocks.LILY_PAD.defaultBlockState(),3);level.updateNeighborsAt(pos,Blocks.WATER);
    }
}
