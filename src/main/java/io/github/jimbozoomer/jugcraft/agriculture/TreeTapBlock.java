package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** One tap per rooted trunk; four minute-sized fill steps per bottle, with optional bucket storage. */
public final class TreeTapBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Keep the old property and its units so existing 0..4 saves retain their contents.
    public static final IntegerProperty LATEX = IntegerProperty.create("latex", 0, 32);
    public static final BooleanProperty UPGRADED = BooleanProperty.create("upgraded");
    public static final int STEPS_PER_BOTTLE = 4;
    public static final int INTERVAL = 1200;
    public TreeTapBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LATEX, 0).setValue(UPGRADED, false));
    }
    public static int capacity(BlockState state) { return state.getValue(UPGRADED) ? 32 : 4; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(UPGRADED)) return switch (state.getValue(FACING)) {
            case NORTH -> box(3, 0, 3, 13, 13, 16);
            case SOUTH -> box(3, 0, 0, 13, 13, 13);
            case WEST -> box(3, 0, 3, 16, 13, 13);
            default -> box(0, 0, 3, 13, 13, 13);
        };
        return switch (state.getValue(FACING)) {
            case NORTH -> box(4, 1, 8, 12, 12, 16);
            case SOUTH -> box(4, 1, 0, 12, 12, 8);
            case WEST -> box(8, 1, 4, 16, 12, 12);
            default -> box(0, 1, 4, 8, 12, 12);
        };
    }
    private static boolean livingLog(BlockState state) {
        return state.getBlock() instanceof RubberLogBlock && state.getValue(RubberLogBlock.NATURAL)
                && state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y;
    }
    public static boolean productive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos trunk = pos.relative(facing.getOpposite());
        if (!livingLog(level.getBlockState(trunk))) return false;
        BlockPos base = trunk;
        for (int n = 0; n < 16 && livingLog(level.getBlockState(base.below())); n++) base = base.below();
        if (trunk.getY() - base.getY() > 2 || !level.getBlockState(base.below()).is(BlockTags.DIRT)) return false;
        BlockPos top = base;
        for (int n = 0; n < 16 && livingLog(level.getBlockState(top.above())); n++) top = top.above();
        if (top.getY() - base.getY() < 7) return false;
        // At most 12 tap positions; duplicate taps cannot multiply one tree's production.
        for (int y = 0; y <= 2; y++) for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos other = base.above(y).relative(side);
            BlockState tap = level.getBlockState(other);
            if (!other.equals(pos) && tap.getBlock() instanceof TreeTapBlock && tap.getValue(FACING) == side) return false;
        }
        int leaves = 0;
        for (BlockPos at : BlockPos.betweenClosed(top.offset(-2, -2, -2), top.offset(2, 2, 2))) {
            BlockState leaf = level.getBlockState(at);
            if (leaf.is(JugcraftAgriculture.block("rubber_leaves")) && !leaf.getValue(LeavesBlock.PERSISTENT)
                    && leaf.getValue(LeavesBlock.DISTANCE) < 7) leaves++;
        }
        return leaves >= 8;
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side = context.getClickedFace();
        if (!side.getAxis().isHorizontal()) return null;
        return productive(context.getLevel(), context.getClickedPos(), side) ? defaultBlockState().setValue(FACING, side) : null;
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return livingLog(level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())));
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos other, BlockState neighbor, RandomSource random) {
        if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, other, neighbor, random);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide() && !old.is(this)) level.scheduleTick(pos, this, INTERVAL);
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) return;
        if (current.getValue(LATEX) < capacity(current) && productive(level, pos, current.getValue(FACING))) {
            current = current.setValue(LATEX, current.getValue(LATEX) + 1);
            level.setBlock(pos, current, Block.UPDATE_CLIENTS);
        }
        if (current.getValue(LATEX) < capacity(current)) level.scheduleTick(pos, this, INTERVAL);
    }
    private void resume(Level level, BlockPos pos) {
        // Changing only properties retains the original scheduled tick, including time within a fill step.
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, INTERVAL);
    }
    private static void showFill(Player player, BlockState state) {
        player.sendOverlayMessage(Component.translatable("message.jugcraft.tree_tap.fill",
                state.getValue(LATEX) / STEPS_PER_BOTTLE, capacity(state) / STEPS_PER_BOTTLE,
                (state.getValue(LATEX) % STEPS_PER_BOTTLE) * 25));
    }
    private static int bottleSpace(Player player, ItemStack held, InteractionHand hand, int available) {
        var result = new ItemStack(JugcraftAgriculture.item("latex_bottle"));
        int room = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            var existing = player.getInventory().getItem(slot);
            if (existing.isEmpty()) room += result.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(existing, result)) room += existing.getMaxStackSize() - existing.getCount();
        }
        if (!player.getAbilities().instabuild && hand == InteractionHand.MAIN_HAND && held.getCount() <= available)
            room += result.getMaxStackSize(); // Consuming the entire held stack frees its hotbar slot.
        return room;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) return InteractionResult.PASS;
        if (stack.is(Items.BUCKET)) {
            if (!level.isClientSide()) {
                if (!current.getValue(UPGRADED)) {
                    current = current.setValue(UPGRADED, true);
                    level.setBlock(pos, current, Block.UPDATE_ALL);
                    stack.consume(1, player);
                    resume(level, pos);
                }
                showFill(player, current);
            }
            return InteractionResult.SUCCESS;
        }
        if (!stack.is(Items.GLASS_BOTTLE)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            int count = Math.min(stack.getCount(), current.getValue(LATEX) / STEPS_PER_BOTTLE);
            count = Math.min(count, bottleSpace(player, stack, hand, count));
            if (count > 0) {
                current = current.setValue(LATEX, current.getValue(LATEX) - count * STEPS_PER_BOTTLE);
                level.setBlock(pos, current, Block.UPDATE_CLIENTS);
                stack.consume(count, player);
                var bottles = new ItemStack(JugcraftAgriculture.item("latex_bottle"), count);
                if (!player.getInventory().add(bottles)) Block.popResource(level, pos, bottles);
                resume(level, pos);
            } else if (current.getValue(LATEX) >= STEPS_PER_BOTTLE) {
                player.sendOverlayMessage(Component.translatable("message.jugcraft.tree_tap.inventory_full"));
                return InteractionResult.SUCCESS;
            }
            showFill(player, current);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) showFill(player, level.getBlockState(pos));
        return InteractionResult.SUCCESS;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, LATEX, UPGRADED); }
}
