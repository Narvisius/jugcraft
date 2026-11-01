package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Source six-direction rake patterns with native falling sand and durability-paid reshaping. */
public final class WildRakedSand extends FallingBlock {
    public enum Shape implements StringRepresentable {NORTH_SOUTH,EAST_WEST,NORTH_EAST,NORTH_WEST,SOUTH_EAST,SOUTH_WEST;public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}}
    public static final EnumProperty<Shape> SHAPE=EnumProperty.create("shape",Shape.class);
    @Override public int getDustColor(BlockState s,BlockGetter l,BlockPos p){return red?0xFFA65C30:0xFFD8CB9B;}
    private final boolean red;
    public WildRakedSand(boolean red,Properties p){super(p);this.red=red;registerDefaultState(defaultBlockState().setValue(SHAPE,Shape.NORTH_SOUTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(SHAPE);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return connected(c.getLevel(),c.getClickedPos(),defaultBlockState().setValue(SHAPE,c.getHorizontalDirection().getAxis()==Direction.Axis.X?Shape.EAST_WEST:Shape.NORTH_SOUTH));}
    private static BlockState connected(LevelReader l,BlockPos p,BlockState s){boolean n=l.getBlockState(p.north()).getBlock() instanceof WildRakedSand,so=l.getBlockState(p.south()).getBlock() instanceof WildRakedSand,e=l.getBlockState(p.east()).getBlock() instanceof WildRakedSand,w=l.getBlockState(p.west()).getBlock() instanceof WildRakedSand;Shape shape=s.getValue(SHAPE);if((n||so)&&!e&&!w)shape=Shape.NORTH_SOUTH;else if((e||w)&&!n&&!so)shape=Shape.EAST_WEST;else if(n&&e&&!so&&!w)shape=Shape.NORTH_EAST;else if(n&&w&&!so&&!e)shape=Shape.NORTH_WEST;else if(so&&e&&!n&&!w)shape=Shape.SOUTH_EAST;else if(so&&w&&!n&&!e)shape=Shape.SOUTH_WEST;return s.setValue(SHAPE,shape);}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState other,RandomSource r){super.updateShape(s,l,ticks,p,d,q,other,r);return connected(l,p,s);}
    @Override protected void tick(BlockState s,net.minecraft.server.level.ServerLevel l,BlockPos p,RandomSource r){if(isFree(l.getBlockState(p.below()))&&p.getY()>=l.getMinY())FallingBlockEntity.fall(l,p,(red?Blocks.RED_SAND:Blocks.SAND).defaultBlockState());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return orient(s,d->r.rotate(d));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return orient(s,d->m.mirror(d));}
    private static BlockState orient(BlockState s,java.util.function.UnaryOperator<Direction> transform){var shape=s.getValue(SHAPE);Direction a=switch(shape){case EAST_WEST->Direction.EAST;case NORTH_SOUTH,NORTH_EAST,NORTH_WEST->Direction.NORTH;default->Direction.SOUTH;};Direction b=switch(shape){case NORTH_SOUTH->Direction.SOUTH;case EAST_WEST,NORTH_WEST,SOUTH_WEST->Direction.WEST;default->Direction.EAST;};a=transform.apply(a);b=transform.apply(b);Shape next=a.getAxis()==b.getAxis()?(a.getAxis()==Direction.Axis.X?Shape.EAST_WEST:Shape.NORTH_SOUTH):(a==Direction.NORTH||b==Direction.NORTH)?(a==Direction.EAST||b==Direction.EAST?Shape.NORTH_EAST:Shape.NORTH_WEST):(a==Direction.EAST||b==Direction.EAST?Shape.SOUTH_EAST:Shape.SOUTH_WEST);return s.setValue(SHAPE,next);}
    public static final class Rake extends Item {
        public Rake(Properties p){super(p.durability(64));}
        @Override public InteractionResult useOn(UseOnContext c){var s=c.getLevel().getBlockState(c.getClickedPos());if(c.getClickedFace()!=Direction.UP||!c.getLevel().isEmptyBlock(c.getClickedPos().above())||!s.is(Blocks.SAND)&&!s.is(Blocks.RED_SAND)&&!(s.getBlock() instanceof WildRakedSand))return InteractionResult.PASS;if(c.getPlayer() instanceof ServerPlayer p){if(!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;var next=s.getBlock() instanceof WildRakedSand?s.cycle(SHAPE):OwnerWilds.block(s.is(Blocks.RED_SAND)?"red_raked_sand":"raked_sand").defaultBlockState().setValue(SHAPE,p.getDirection().getAxis()==Direction.Axis.X?Shape.EAST_WEST:Shape.NORTH_SOUTH);p.level().setBlockAndUpdate(c.getClickedPos(),next);c.getItemInHand().hurtAndBreak(1,p,c.getHand().asEquipmentSlot());p.level().playSound(null,c.getClickedPos(),net.minecraft.sounds.SoundEvents.HOE_TILL.value(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);}return InteractionResult.SUCCESS;}
    }
}
