package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Connections, safe grouped gates and authored stacked fence silhouettes. */
final class OwnerFenceBlocks {
    private OwnerFenceBlocks() { }
    enum Part implements StringRepresentable {
        TOP, MIDDLE, BOTTOM;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    static final EnumProperty<Part> PART = EnumProperty.create("fencepart", Part.class);
    static final EnumProperty<Part> TWO_PART = EnumProperty.create("fencepart", Part.class, Part.TOP, Part.BOTTOM);

    static class Fence extends FenceBlock {
        private final boolean thick, wired;
        Fence(Properties properties, boolean thick, boolean wired) { super(properties); this.thick = thick; this.wired = wired; }
        @Override public boolean connectsTo(BlockState neighbor, boolean sturdy, Direction direction) {
            return neighbor.getBlock() instanceof Fence || neighbor.getBlock() instanceof WallBlock
                || super.connectsTo(neighbor, sturdy, direction);
        }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (!thick) return super.getShape(state, level, pos, context);
            VoxelShape shape = Block.box(4, 0, 4, 12, 16, 12);
            if (state.getValue(NORTH)) shape = Shapes.or(shape, Block.box(5, 0, 0, 11, 16, 8));
            if (state.getValue(SOUTH)) shape = Shapes.or(shape, Block.box(5, 0, 8, 11, 16, 16));
            if (state.getValue(WEST)) shape = Shapes.or(shape, Block.box(0, 0, 5, 8, 16, 11));
            if (state.getValue(EAST)) shape = Shapes.or(shape, Block.box(8, 0, 5, 16, 16, 11));
            return shape;
        }
        @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                InsideBlockEffectApplier effects, boolean pastEdges) {
            super.entityInside(state, level, pos, entity, effects, pastEdges);
            if (wired && level instanceof ServerLevel server && entity instanceof LivingEntity living)
                living.hurtServer(server, server.damageSources().cactus(), 2);
        }
    }

    static class Stacked extends Fence {
        Stacked(Properties properties) {
            super(properties, false, false);
            registerDefaultState(defaultBlockState().setValue(partProperty(), Part.TOP));
        }
        EnumProperty<Part> partProperty() { return TWO_PART; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder); builder.add(partProperty());
        }
        private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
            Part part = !level.getBlockState(pos.above()).is(this) ? Part.TOP
                : partProperty() == PART && level.getBlockState(pos.below()).is(this) ? Part.MIDDLE : Part.BOTTOM;
            return state.setValue(partProperty(), part);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return connected(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return connected(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), level, pos);
        }
    }
    static final class ThreePartFence extends Stacked {
        ThreePartFence(Properties properties) { super(properties); }
        @Override EnumProperty<Part> partProperty() { return PART; }
    }

    static final class Gate extends FenceGateBlock {
        static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
        Gate(Properties properties) {
            super(WoodType.OAK, properties);
            registerDefaultState(defaultBlockState().setValue(TWO_PART, Part.TOP).setValue(HINGE, DoorHingeSide.LEFT));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder); builder.add(TWO_PART, HINGE);
        }
        private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
            return state.setValue(TWO_PART, level.getBlockState(pos.above()).is(this) ? Part.BOTTOM : Part.TOP);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            BlockPos pos = context.getClickedPos(); Direction side = state.getValue(FACING).getClockWise();
            double x = context.getClickLocation().x - pos.getX() - .5;
            double z = context.getClickLocation().z - pos.getZ() - .5;
            DoorHingeSide hinge = x * side.getStepX() + z * side.getStepZ() > 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
            BlockState below = context.getLevel().getBlockState(pos.below());
            if (below.is(this) && below.getValue(FACING) == state.getValue(FACING)) hinge = below.getValue(HINGE);
            return connected(state.setValue(HINGE, hinge), context.getLevel(), pos);
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return connected(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), level, pos);
        }
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(level instanceof ServerLevel server) || player.isSpectator()) return InteractionResult.FAIL;
            // Bounded connected traversal, no chunk loads and no recursive
            // mutation before every member has passed the same authority check.
            Set<BlockPos> members = new LinkedHashSet<>(); ArrayDeque<BlockPos> pending = new ArrayDeque<>();
            pending.add(pos); members.add(pos);
            Direction along = state.getValue(FACING).getClockWise();
            while (!pending.isEmpty()) {
                BlockPos here = pending.removeFirst();
                if (!Authority.mayChange(server, player, here) && !TownProtection.allowsPublicUse(player, level, here))
                    return InteractionResult.FAIL;
                for (Direction direction : new Direction[] {Direction.UP, Direction.DOWN, along, along.getOpposite()}) {
                    BlockPos next = here.relative(direction);
                    if (!level.hasChunkAt(next) || members.contains(next)) continue;
                    BlockState neighbor = level.getBlockState(next);
                    if (!neighbor.is(this) || neighbor.getValue(FACING) != state.getValue(FACING)) continue;
                    if (members.size() >= 64) return InteractionResult.FAIL;
                    members.add(next); pending.add(next);
                }
            }
            InteractionResult result = super.useWithoutItem(state, level, pos, player, hit);
            BlockState changed = level.getBlockState(pos);
            for (BlockPos member : members) if (!member.equals(pos)) {
                BlockState old = level.getBlockState(member);
                level.setBlockAndUpdate(member, old.setValue(OPEN, changed.getValue(OPEN)));
            }
            return result;
        }
        @Override protected BlockState mirror(BlockState state, Mirror mirror) {
            BlockState result = super.mirror(state, mirror);
            return mirror == Mirror.NONE ? result : result.cycle(HINGE);
        }
    }
}
