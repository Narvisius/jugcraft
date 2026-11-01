package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.material.PushReaction;

/** Native placement and redstone for the owner's supplied factory artwork. */
final class OwnerFactoryBlocks {
    private OwnerFactoryBlocks() { }

    static class Facing extends HorizontalDirectionalBlock {
        Facing(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
    }

    static final class PoweredFan extends Facing {
        static final BooleanProperty ON = BooleanProperty.create("on");
        PoweredFan(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(ON, false));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder); builder.add(ON);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return super.getStateForPlacement(context).setValue(ON, context.getLevel().hasNeighborSignal(context.getClickedPos()));
        }
        @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                Orientation orientation, boolean moving) {
            if (!level.isClientSide()) {
                boolean powered = level.hasNeighborSignal(pos);
                if (state.getValue(ON) != powered) level.setBlockAndUpdate(pos, state.setValue(ON, powered));
            }
        }
    }

    /** The source mesh spans two by two cells, so placement reserves all four. */
    static final class LargeFan extends Facing {
        static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, 1);
        static final IntegerProperty ROW = IntegerProperty.create("row", 0, 1);
        LargeFan(Properties properties) {
            super(properties.pushReaction(PushReaction.IMMOVEABLE));
            registerDefaultState(defaultBlockState().setValue(COLUMN, 0).setValue(ROW, 0));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder); builder.add(COLUMN, ROW);
        }
        static Direction along(BlockState state) {
            return state.getValue(FACING).getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        }
        static BlockPos origin(BlockState state, BlockPos pos) {
            return pos.relative(along(state), -state.getValue(COLUMN)).below(state.getValue(ROW));
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            Level level = context.getLevel(); BlockPos origin = context.getClickedPos();
            for (int row = 0; row < 2; row++) for (int column = 0; column < 2; column++) {
                BlockPos cell = origin.relative(along(state), column).above(row);
                if (level.isOutsideBuildHeight(cell) || !level.getWorldBorder().isWithinBounds(cell)
                        || !level.getBlockState(cell).canBeReplaced(context)) return null;
                if (level instanceof ServerLevel server && (context.getPlayer() == null
                        || !Authority.mayChange(server, context.getPlayer(), cell))) return null;
            }
            return state;
        }
        @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            // Delay neighbor notifications until the complete assembly exists.
            for (int row = 0; row < 2; row++) for (int column = 0; column < 2; column++) {
                if (row == 0 && column == 0) continue;
                level.setBlock(pos.relative(along(state), column).above(row),
                        state.setValue(COLUMN, column).setValue(ROW, row), Block.UPDATE_CLIENTS);
            }
            for (int row = 0; row < 2; row++) for (int column = 0; column < 2; column++)
                level.updateNeighborsAt(pos.relative(along(state), column).above(row), this);
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            BlockPos origin = origin(state, pos);
            // Validate the changed member only, allowing initial assembly order.
            for (int row = 0; row < 2; row++) for (int column = 0; column < 2; column++) {
                if (!origin.relative(along(state), column).above(row).equals(neighborPos)) continue;
                if (!neighbor.is(this) || neighbor.getValue(FACING) != state.getValue(FACING)
                        || neighbor.getValue(COLUMN) != column || neighbor.getValue(ROW) != row)
                    return Blocks.AIR.defaultBlockState();
            }
            return state;
        }
        @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            if (!level.isClientSide() && player.preventsBlockDrops()) {
                BlockPos origin = origin(state, pos);
                // Remove the only loot-bearing member first, suppressing its
                // drops before normal neighbor teardown reaches the rest.
                if (!origin.equals(pos) && level.getBlockState(origin).is(this))
                    level.setBlock(origin, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
            return super.playerWillDestroy(level, pos, state, player);
        }
        @Override protected BlockState rotate(BlockState state, Rotation rotation) {
            Direction transformed = rotation.rotate(along(state));
            BlockState rotated = super.rotate(state, rotation);
            return transformed == along(rotated) ? rotated : rotated.setValue(COLUMN, 1 - state.getValue(COLUMN));
        }
        @Override protected BlockState mirror(BlockState state, Mirror mirror) {
            Direction transformed = mirror.mirror(along(state));
            BlockState mirrored = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
            return transformed == along(mirrored) ? mirrored : mirrored.setValue(COLUMN, 1 - state.getValue(COLUMN));
        }
    }
}
