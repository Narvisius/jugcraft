package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Placement and joining for the properties used by the supplied decorative meshes. */
public final class OwnerDecorVariants {
    private OwnerDecorVariants() {}
    public static final class Facing extends DirectionalBlock {
        public Facing(Properties p) {super(p); registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) {return defaultBlockState().setValue(FACING,c.getClickedFace());}
    }
    public static final class Brace extends Block {
        public static final BooleanProperty FLIPPED=BooleanProperty.create("flipped");
        public Brace(Properties p) {super(p); registerDefaultState(defaultBlockState().setValue(FLIPPED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(FLIPPED);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) {return defaultBlockState().setValue(FLIPPED,c.getHorizontalDirection().getAxis()==Direction.Axis.X);}
        @Override protected BlockState mirror(BlockState s,Mirror m) {return m==Mirror.NONE?s:s.cycle(FLIPPED);}
        @Override protected BlockState rotate(BlockState s,Rotation r) {return r==Rotation.CLOCKWISE_90||r==Rotation.COUNTERCLOCKWISE_90?s.cycle(FLIPPED):s;}
    }
    public static final class AxisSlab extends SlabBlock {
        public static final EnumProperty<Direction.Axis> AXIS=EnumProperty.create("axis",Direction.Axis.class,Direction.Axis.X,Direction.Axis.Z);
        public AxisSlab(Properties p) {super(p); registerDefaultState(defaultBlockState().setValue(AXIS,Direction.Axis.Z));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {super.createBlockStateDefinition(b);b.add(AXIS);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) {var s=super.getStateForPlacement(c);return s==null?null:c.getLevel().getBlockState(c.getClickedPos()).is(this)?s:s.setValue(AXIS,c.getHorizontalDirection().getAxis());}
        @Override protected BlockState rotate(BlockState s,Rotation r) {return r==Rotation.CLOCKWISE_90||r==Rotation.COUNTERCLOCKWISE_90?s.setValue(AXIS,s.getValue(AXIS)==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X):s;}
    }
    public enum Segment implements StringRepresentable {
        SINGLE, BOTTOM, MIDDLE, TOP;
        @Override public String getSerializedName() {return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public static final class JoinedPillar extends RotatedPillarBlock {
        public static final EnumProperty<Segment> TYPE=EnumProperty.create("type",Segment.class);
        public JoinedPillar(Properties p) {super(p);registerDefaultState(defaultBlockState().setValue(TYPE,Segment.SINGLE));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {super.createBlockStateDefinition(b);b.add(TYPE);}
        private BlockState joined(BlockState s,LevelReader l,BlockPos p) {
            var axis=s.getValue(AXIS);var d=Direction.fromAxisAndDirection(axis,Direction.AxisDirection.POSITIVE);
            var a=l.getBlockState(p.relative(d));var z=l.getBlockState(p.relative(d.getOpposite()));
            boolean ahead=a.is(this)&&a.getValue(AXIS)==axis,behind=z.is(this)&&z.getValue(AXIS)==axis;
            return s.setValue(TYPE,ahead?(behind?Segment.MIDDLE:Segment.BOTTOM):(behind?Segment.TOP:Segment.SINGLE));
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) {return joined(super.getStateForPlacement(c),c.getLevel(),c.getClickedPos());}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState n,RandomSource random) {return joined(s,l,p);}
    }
}
