package io.github.jimbozoomer.jugcraft.fluid;

import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.*;

/** Original narrow glass tank silhouette and lower-join skin, using the shared conserved tank inventory. */
public final class WorkshopGlassTankBlock extends FluidTankBlock {
    public static final BooleanProperty JOINED_BELOW=BooleanProperty.create("joined_below");
    public WorkshopGlassTankBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(JOINED_BELOW,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(JOINED_BELOW);}
    private boolean joined(LevelReader world,BlockPos pos){return world.hasChunkAt(pos.below())&&world.getBlockState(pos.below()).getBlock()==this&&(!(world instanceof Level l)||ClaimTransfers.allowed(l,pos,pos.below()));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(JOINED_BELOW,joined(c.getLevel(),c.getClickedPos()));}
    @Override protected BlockState updateShape(BlockState state,LevelReader world,ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos neighbor,BlockState other,RandomSource random){return side==Direction.DOWN?state.setValue(JOINED_BELOW,joined(world,pos)):state;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return Block.box(2,0,2,14,16,14);}
}
