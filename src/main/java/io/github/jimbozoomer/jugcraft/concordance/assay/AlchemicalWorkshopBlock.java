package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.jspecify.annotations.Nullable;

public final class AlchemicalWorkshopBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    public final boolean collector;
    public final int tier;
    public AlchemicalWorkshopBlock(Properties properties, boolean collector, int tier) {
        super(properties); this.collector = collector; this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.OPEN,false).setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ACTIVE,BlockStateProperties.OPEN); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override protected RenderShape getRenderShape(BlockState state) { return collector?RenderShape.MODEL:RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return collector ? Shapes.block() : Block.box(1, 0, 1, 15, 15, 15);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AlchemicalWorkshopBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, AlchemicalWorkshops.ENTITY, AlchemicalWorkshopBlockEntity::tick);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof AlchemicalWorkshopBlockEntity be) be.placedBy(player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p && level.getBlockEntity(pos) instanceof AlchemicalWorkshopBlockEntity be
                && RateGate.allow(p, "alchemy_workshop_open", 4)) {
            if (!be.mayUse(p)) {
                p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.workshop.denied"));
                return InteractionResult.FAIL;
            }
            p.openMenu(new SimpleMenuProvider((id, inv, who) -> new AlchemicalWorkshopMenu(id, inv, be), getName()));
        }
        return InteractionResult.SUCCESS;
    }
}
