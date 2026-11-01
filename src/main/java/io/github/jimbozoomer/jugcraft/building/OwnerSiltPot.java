package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Nine-slot pots retain serialized cargo while falling, with no intermediate spill. */
public final class OwnerSiltPot extends FallingBlock implements EntityBlock, SimpleWaterloggedBlock {
    public static BlockEntityType<Storage> TYPE;
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 1, 2, 14, 13, 14), Block.box(3, 14, 3, 13, 16, 13), Block.box(4, 0, 4, 12, 14, 12));
    OwnerSiltPot(Properties p) { super(p); registerDefaultState(defaultBlockState().setValue(FILLED, false).setValue(WATERLOGGED, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FILLED, WATERLOGGED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(WATERLOGGED, c.getLevel().getFluidState(c.getClickedPos()).getType() == Fluids.WATER); }
    @Override protected FluidState getFluidState(BlockState s) { return s.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(s); }
    @Override protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess ticks, BlockPos p, Direction d, BlockPos n, BlockState other, RandomSource r) {
        if (s.getValue(WATERLOGGED)) ticks.scheduleTick(p, Fluids.WATER, Fluids.WATER.getTickDelay(l));
        return super.updateShape(s, l, ticks, p, d, n, other, r);
    }
    @Override public int getDustColor(BlockState s, BlockGetter l, BlockPos p) { return 0xA97B54; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new Storage(p, s); }
    @Override protected void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!FallingBlock.isFree(level.getBlockState(pos.below())) || pos.getY() < level.getMinY()) return;
        if (!(level.getBlockEntity(pos) instanceof Storage storage)) return;
        // Resolve unopened loot before snapshotting; clear the live container so
        // BlockEntity.preRemoveSideEffects cannot duplicate its falling contents.
        storage.unpackLootTable(null);
        var saved = storage.saveWithoutMetadata(level.registryAccess());
        storage.clearContent();
        var falling = FallingBlockEntity.fall(level, pos, s);
        falling.blockData = saved;
    }
    @Override public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity entity) {
        if (level.isClientSide() || entity.blockData == null) return;
        var cargo = new Storage(pos, entity.getBlockState()); cargo.setLevel(level);
        cargo.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entity.blockData));
        Containers.dropContents(level, pos, cargo); entity.blockData = null;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!Authority.mayChange(level, player, pos)) return InteractionResult.FAIL;
        boolean filled = state.getValue(FILLED);
        if (filled && stack.getItem() instanceof BlockItem item && item.getBlock() instanceof FlowerBlock && hit.getDirection() == Direction.UP)
            return InteractionResult.PASS;
        if ((!filled && stack.is(Items.ROOTED_DIRT)) || (filled && stack.is(ItemTags.SHOVELS))) {
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, state.setValue(FILLED, !filled));
                if (filled) { if (!player.isCreative()) stack.hurtAndBreak(1, player, hand.asEquipmentSlot()); }
                else stack.consume(1, player);
                level.playSound(null, pos, filled ? SoundEvents.ROOTED_DIRT_BREAK : SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1, 1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        if (s.getValue(FILLED)) return InteractionResult.PASS;
        if (!Authority.mayChange(l, player, p)) return InteractionResult.FAIL;
        if (!l.isClientSide() && l.getBlockEntity(p) instanceof Storage storage) player.openMenu(storage);
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) { return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos)); }
    @Override protected void affectNeighborsAfterRemoval(BlockState s, ServerLevel l, BlockPos p, boolean moved) { Containers.updateNeighboursAfterDestroy(s, l, p); }
    public static final class Storage extends DispenserBlockEntity {
        Storage(BlockPos p, BlockState s) { super(TYPE, p, s); }
        @Override protected Component getDefaultName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }
        @Override public boolean stillValid(Player player) { return !getBlockState().getValue(FILLED) && super.stillValid(player) && Authority.mayChange(level, player, worldPosition); }
    }
}
