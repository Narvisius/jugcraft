package io.github.jimbozoomer.jugcraft.jimtafacts.computers;

import io.github.jimbozoomer.jugcraft.control.DataConnectable;
import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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

public class ComputerBlock extends Block implements EntityBlock, DataConnectable, EnergyConnectable {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public final String kind;
    public ComputerBlock(Properties properties, String kind) {
        super(properties); this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, LIT); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction face = state.getValue(FACING);
        if (kind.startsWith("turtle_")) {
            VoxelShape body = Block.box(2, 2, 2, 14, 14, 14);
            VoxelShape backpack = switch (face) {
                case NORTH -> Block.box(3, 6, 13, 13, 13, 15);
                case SOUTH -> Block.box(3, 6, 1, 13, 13, 3);
                case EAST -> Block.box(1, 6, 3, 3, 13, 13);
                default -> Block.box(13, 6, 3, 15, 13, 13);
            };
            return Shapes.or(body, backpack);
        }
        if (kind.contains("modem") && !kind.equals("wired_modem_full")) return switch (face) {
            case NORTH -> Block.box(2, 2, 0, 14, 14, 3);
            case SOUTH -> Block.box(2, 2, 13, 14, 14, 16);
            case EAST -> Block.box(13, 2, 2, 16, 14, 14);
            default -> Block.box(0, 2, 2, 3, 14, 14);
        };
        return Shapes.block();
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ComputerNode(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return kind.startsWith("turtle_") ? RenderShape.INVISIBLE : RenderShape.MODEL; }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer p && level.getBlockEntity(pos) instanceof ComputerNode node) node.owner(p.getUUID());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p && level.getBlockEntity(pos) instanceof ComputerNode node) node.open(p);
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof ComputerNode node ? node.signal(side.getOpposite()) : 0;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == WorkshopComputers.NODE ? (world, pos, s, be) -> ((ComputerNode) be).tick() : null;
    }
}
