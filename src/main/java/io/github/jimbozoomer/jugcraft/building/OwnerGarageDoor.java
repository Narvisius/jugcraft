package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Retracting columns retain their occupied cells, never overwrite new builds. */
final class OwnerGarageDoor extends HorizontalDirectionalBlock {
    enum Part implements StringRepresentable {
        TOP, MIDDLE;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    static final BooleanProperty OPEN = BlockStateProperties.OPEN, POWERED = BlockStateProperties.POWERED;
    static final int MAX_HEIGHT = 16, MAX_COLUMNS = 16;
    OwnerGarageDoor(Properties properties) {
        super(properties.pushReaction(PushReaction.IMMOVEABLE));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PART, Part.TOP)
            .setValue(OPEN, false).setValue(POWERED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OPEN, POWERED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel(); BlockPos pos = context.getClickedPos();
        int height = 1;
        while (height < MAX_HEIGHT && level.getBlockState(pos.below(height)).isAir()) height++;
        if (level.getBlockState(pos.below(height)).isAir()) return null;
        for (int y = 0; y < height; y++) {
            BlockPos cell = pos.below(y);
            if (!level.isInWorldBounds(cell) || !level.getWorldBorder().isWithinBounds(cell)) return null;
            if (level instanceof ServerLevel server && !Authority.mayChange(server, context.getPlayer(), cell)) return null;
        }
        boolean powered = level.hasNeighborSignal(pos);
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise())
            .setValue(POWERED, powered).setValue(OPEN, powered);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        int height = 1;
        while (height < MAX_HEIGHT && level.getBlockState(pos.below(height)).isAir()) {
            level.setBlock(pos.below(height), state.setValue(PART, Part.MIDDLE), Block.UPDATE_CLIENTS);
            height++;
        }
        for (int y = 0; y < height; y++) level.updateNeighborsAt(pos.below(y), this);
    }
    private BlockPos top(LevelReader level, BlockPos pos) {
        BlockState initial = level.getBlockState(pos); if (!initial.is(this)) return null;
        for (int y = 0; y < MAX_HEIGHT; y++) {
            BlockPos next = pos.above(y); BlockState state = level.getBlockState(next);
            if (!state.is(this) || state.getValue(FACING) != initial.getValue(FACING)) return null;
            if (state.getValue(PART) == Part.TOP) return next;
        }
        return null;
    }
    private List<BlockPos> column(LevelReader level, BlockPos root) {
        List<BlockPos> result = new ArrayList<>(); BlockState top = level.getBlockState(root);
        if (!top.is(this) || top.getValue(PART) != Part.TOP) return result;
        result.add(root);
        for (int y = 1; y < MAX_HEIGHT; y++) {
            BlockPos pos = root.below(y); BlockState state = level.getBlockState(pos);
            if (!state.is(this) || state.getValue(PART) != Part.MIDDLE || state.getValue(FACING) != top.getValue(FACING)) break;
            result.add(pos);
        }
        return result;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || player.isSpectator()) return InteractionResult.FAIL;
        BlockPos root = top(level, pos); if (root == null) return InteractionResult.FAIL;
        Set<BlockPos> roots = new LinkedHashSet<>(); roots.add(root);
        ArrayDeque<BlockPos> pending = new ArrayDeque<>(); pending.add(root);
        List<BlockPos> members = new ArrayList<>(); Direction along = state.getValue(FACING);
        while (!pending.isEmpty()) {
            BlockPos here = pending.removeFirst(); members.addAll(column(level, here));
            for (Direction step : new Direction[] {along, along.getOpposite()}) {
                BlockPos next = here.relative(step);
                if (!level.hasChunkAt(next) || roots.contains(next)) continue;
                BlockState neighbor = level.getBlockState(next);
                if (!neighbor.is(this) || neighbor.getValue(PART) != Part.TOP || neighbor.getValue(FACING) != along) continue;
                if (roots.size() >= MAX_COLUMNS) return InteractionResult.FAIL;
                roots.add(next); pending.add(next);
            }
        }
        for (BlockPos member : members)
            if (!Authority.mayChange(server, player, member) && !TownProtection.allowsPublicUse(player, level, member)) return InteractionResult.FAIL;
        apply(level, members, !state.getValue(OPEN), null);
        level.playSound(null, root, state.getValue(OPEN) ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, .6f, .65f);
        return InteractionResult.SUCCESS;
    }
    private void apply(Level level, List<BlockPos> members, boolean open, Boolean powered) {
        // First save all states without neighbor callbacks, then notify. This
        // prevents a redstone callback seeing a half-updated column.
        for (BlockPos member : members) {
            BlockState current = level.getBlockState(member); if (!current.is(this)) continue;
            current = current.setValue(OPEN, open);
            if (powered != null) current = current.setValue(POWERED, powered);
            level.setBlock(member, current, Block.UPDATE_CLIENTS);
        }
        for (BlockPos member : members) level.updateNeighborsAt(member, this);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean moving) {
        if (level.isClientSide()) return;
        BlockPos root = top(level, pos); if (root == null) return;
        List<BlockPos> members = column(level, root);
        boolean powered = members.stream().anyMatch(level::hasNeighborSignal);
        if (level.getBlockState(root).getValue(POWERED) != powered) apply(level, members, powered, powered);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (state.getValue(PART) == Part.MIDDLE && direction == Direction.UP
                && (!neighbor.is(this) || neighbor.getValue(FACING) != state.getValue(FACING))) return Blocks.AIR.defaultBlockState();
        // Losing a middle segment takes down the hanging column above; its top
        // drops the single paid item and every remaining segment has empty loot.
        if (direction == Direction.DOWN && neighbor.isAir()) return Blocks.AIR.defaultBlockState();
        return state;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()) {
            BlockPos root = top(level, pos);
            if (root != null && !root.equals(pos)) level.setBlock(root, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN)) return state.getValue(PART) == Part.TOP ? Block.box(0, 14, 0, 16, 16, 16) : Shapes.empty();
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? Block.box(0, 0, 6, 16, 16, 10) : Block.box(6, 0, 0, 10, 16, 16);
    }
    @Override protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return state.getValue(OPEN);
    }
}
