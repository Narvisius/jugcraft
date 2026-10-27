package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Connected arms for active endpoints which are not subclasses of a passive pipe. */
public final class TransportEndpointShape {
    private static final BooleanProperty[] FACES={BlockStateProperties.DOWN,BlockStateProperties.UP,BlockStateProperties.NORTH,BlockStateProperties.SOUTH,BlockStateProperties.WEST,BlockStateProperties.EAST};
    private TransportEndpointShape(){}
    public static void define(StateDefinition.Builder<Block,BlockState> b){b.add(FACES);}
    public static BlockState empty(BlockState state){for(var p:FACES)state=state.setValue(p,false);return state;}
    public static boolean fluid(BlockState state){return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("_fluid");}
    public static BlockState connected(BlockGetter level,BlockPos pos,BlockState state){
        for(Direction d:Direction.values()){
            boolean loaded=!(level instanceof Level l)||l.hasChunkAt(pos.relative(d));
            var neighbor=loaded?level.getBlockState(pos.relative(d)):net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            boolean joins=loaded&&(fluid(state)?io.github.jimbozoomer.jugcraft.fluid.FluidPipeBlock.connectsTo(level,pos,d,neighbor):io.github.jimbozoomer.jugcraft.logistics.ItemPipeBlock.connectsTo(level,pos,d,neighbor));
            state=state.setValue(FACES[d.ordinal()],joins);
        }return state;
    }
    public static void refresh(Level level,BlockPos pos){
        var state=level.getBlockState(pos);if(!state.hasProperty(FACES[0]))return;
        var next=connected(level,pos,state);if(next!=state)level.setBlock(pos,next,Block.UPDATE_ALL);
    }
    public static VoxelShape shape(BlockState state,BlockGetter world,BlockPos pos){
        int lo=fluid(state)?6:5,hi=16-lo;var shape=Block.box(lo,lo,lo,hi,hi,hi);
        for(Direction side:Direction.values())if(state.getValue(FACES[side.ordinal()]))shape=Shapes.or(shape,switch(side){
            case NORTH->Block.box(lo,lo,0,hi,hi,lo);case SOUTH->Block.box(lo,lo,hi,hi,hi,16);
            case WEST->Block.box(0,lo,lo,lo,hi,hi);case EAST->Block.box(hi,lo,lo,16,hi,hi);
            case DOWN->Block.box(lo,0,lo,hi,lo,hi);case UP->Block.box(lo,hi,lo,hi,16,hi);
        });return TransportAttachments.shape(shape,world,pos);
    }
}
