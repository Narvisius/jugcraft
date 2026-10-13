package local.peepo;

import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

public final class CompanionBedBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty STACKED = BooleanProperty.create("stacked");
    public CompanionBedBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STACKED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, STACKED); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CompanionBedEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, CompanionBeds.ENTITY, CompanionBedEntity::tick);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean alongZ = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        VoxelShape mattress = alongZ ? Block.box(2, 0, 0, 14, 6, 16) : Block.box(0, 0, 2, 16, 6, 14);
        if (!state.getValue(STACKED)) return mattress;
        for (int a : new int[]{2, 13}) for (int b : new int[]{0, 15})
            mattress = Shapes.or(mattress, alongZ ? Block.box(a, 6, b, a + 1, 16, b + 1) : Block.box(b, 6, a, b + 1, 16, a + 1));
        return mattress;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var pos = context.getClickedPos(); var level = context.getLevel();
        var below = level.getBlockState(pos.below());
        Direction facing = below.getBlock() instanceof CompanionBedBlock ? below.getValue(FACING) : context.getHorizontalDirection().getOpposite();
        var state = defaultBlockState().setValue(FACING, facing).setValue(STACKED, matching(level.getBlockState(pos.above()), facing));
        return state.canSurvive(level, pos) ? state : null;
    }
    static boolean matching(BlockState state, Direction facing) { return state.getBlock() instanceof CompanionBedBlock && state.getValue(FACING) == facing; }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var below = level.getBlockState(pos.below());
        return matching(below, state.getValue(FACING)) || below.isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if(level.getBlockEntity(pos) instanceof CompanionBedEntity bed)bed.invalidateSpace();
        if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return state.setValue(STACKED, matching(level.getBlockState(pos.above()), state.getValue(FACING)));
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
}
