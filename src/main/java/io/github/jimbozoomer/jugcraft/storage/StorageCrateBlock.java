package io.github.jimbozoomer.jugcraft.storage;

import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.logistics.ItemConnectable;
import io.github.jimbozoomer.jugcraft.placement.StructurePermissions;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Front access means crates open under solid blocks and never merge with a neighbour. */
public final class StorageCrateBlock extends BaseEntityBlock implements WorldlyContainerHolder, ItemConnectable {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public final StorageCrateTier tier;
    public StorageCrateBlock(Properties properties, StorageCrateTier tier) {
        super(properties); this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    public static boolean mayUse(@Nullable Level level, BlockPos pos, Player player) {
        return level != null && StructurePermissions.mayChange(level, pos, player)
            && LandRegistry.mayBuild(player, level, pos);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var player = context.getPlayer();
        return player != null && mayUse(context.getLevel(), context.getClickedPos(), player)
            ? defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()) : null;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StorageCrateBlockEntity(pos, state); }
    @Override public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StorageCrateBlockEntity crate ? crate : null;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof StorageCrateUpgradeItem kit) return kit.upgrade(level, pos, player, stack);
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!mayUse(level, pos, player)) return InteractionResult.FAIL;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageCrateBlockEntity crate && crate.stillValid(player)) player.openMenu(crate);
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof StorageCrateBlockEntity crate ? AbstractContainerMenu.getRedstoneSignalFromContainer(crate) : 0;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
}
