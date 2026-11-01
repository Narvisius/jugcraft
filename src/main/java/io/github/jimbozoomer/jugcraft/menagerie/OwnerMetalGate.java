package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Source metal gates: iron follows power; powered gold gates lock their current position. */
final class OwnerMetalGate extends FenceGateBlock implements SimpleWaterloggedBlock {
    private final boolean gold;
    OwnerMetalGate(Properties p,boolean gold){super(WoodType.OAK,p);this.gold=gold;registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.WATERLOGGED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=super.getStateForPlacement(c);return s==null?null:s.setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(net.minecraft.tags.FluidTags.WATER)).setValue(IN_WALL,connected(c.getLevel(),c.getClickedPos(),s.getValue(FACING)));}
    private boolean connected(LevelReader l,BlockPos p,Direction d){var above=l.getBlockState(p.above());return above.isFaceSturdy(l,p.above(),Direction.DOWN)||above.getBlock() instanceof OwnerMetalGate||above.getBlock() instanceof IronBarsBlock||l.getBlockState(p.relative(d.getClockWise())).getBlock() instanceof IronBarsBlock||l.getBlockState(p.relative(d.getCounterClockWise())).getBlock() instanceof IronBarsBlock;}
    @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s){return s.getValue(BlockStateProperties.WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState n,RandomSource r){if(s.getValue(BlockStateProperties.WATERLOGGED))ticks.scheduleTick(p,net.minecraft.world.level.material.Fluids.WATER,5);return s.setValue(IN_WALL,connected(l,p,s.getValue(FACING)));}
    private void sound(Level l,BlockPos p,boolean open){l.playSound(null,p,open?net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN:net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult hit){
        if(!gold||s.getValue(POWERED))return InteractionResult.PASS;if(!Menagerie.mayChange(l,who,p))return InteractionResult.FAIL;
        if(!l.isClientSide()){
            boolean open=!s.getValue(OPEN);l.setBlockAndUpdate(p,s.setValue(OPEN,open));
            for(var q:new BlockPos[]{p.above(),p.below()})if(l.hasChunkAt(q)){var n=l.getBlockState(q);if(n.getBlock() instanceof OwnerMetalGate gate&&gate.gold&&!n.getValue(POWERED)&&n.getValue(FACING)==s.getValue(FACING)&&Menagerie.mayChange(l,who,q))l.setBlockAndUpdate(q,n.setValue(OPEN,open));}
            sound(l,p,open);
        }return InteractionResult.SUCCESS;
    }
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block changed,net.minecraft.world.level.redstone.Orientation orientation,boolean moving){if(l.isClientSide())return;boolean power=l.hasNeighborSignal(p);if(power!=s.getValue(POWERED)){var next=s.setValue(POWERED,power);if(!gold){next=next.setValue(OPEN,power);sound(l,p,power);}l.setBlockAndUpdate(p,next);}}
}
