package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public class DigitalNodeBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);
    public final String kind;
    public static final class Powered extends DigitalNodeBlock implements EnergyConnectable {
        public Powered(Properties p, String kind) { super(p, kind); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(CHARGE); }
    }
    public static final class Interface extends DigitalNodeBlock implements io.github.jimbozoomer.jugcraft.fluid.FluidConnectable, io.github.jimbozoomer.jugcraft.logistics.ItemConnectable {
        public Interface(Properties p, String kind) { super(p, kind); }
    }
    public DigitalNodeBlock(Properties p, String kind) { super(p); this.kind = kind; registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        boolean attachment = kind.endsWith("_bus") || kind.endsWith("_plane") || kind.endsWith("_tunnel") || kind.equals("pattern_provider") || kind.equals("quartz_fiber") || DigitalControls.controls(kind);
        return defaultBlockState().setValue(FACING, attachment ? c.getClickedFace().getOpposite() : c.getHorizontalDirection().getOpposite());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DigitalNode(pos, state); }
    @Override protected boolean triggerEvent(BlockState s, Level l, BlockPos p, int type, int data) { return l.getBlockEntity(p) instanceof DigitalNode node && node.triggerEvent(type, data); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        if (entity instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof DigitalNode node) node.owner(player.getUUID());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof DigitalNode node && node.permitted(server)) {
            if (DigitalDisplays.monitor(kind)) { DigitalDisplays.interact(node, server, InteractionHand.MAIN_HAND); }
            else if (player.isShiftKeyDown() && (kind.endsWith("_bus") || kind.endsWith("_plane"))) {
                node.filter(ItemStack.EMPTY); server.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Transfer filter cleared."));
            } else node.open(server);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DigitalEquipment.Wrench || stack.getItem() instanceof DigitalEquipment.Memory || stack.getItem() instanceof DigitalCellCards.Card || stack.getItem() instanceof DigitalQuantum.Key) return InteractionResult.PASS;
        if (DigitalDisplays.monitor(kind) && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof DigitalNode node) { DigitalDisplays.interact(node, server, hand); return InteractionResult.SUCCESS; }
        if (kind.equals("condenser") && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof DigitalNode node && node.permitted(server)
                && net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil.interactWithFluidStorage(node.condenserStorage, player, hand)) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof DigitalNode node && node.permitted(server)
                && (kind.equals("interface") || kind.equals("me_chest"))
                && net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil.interactWithFluidStorage(new FluidCells.Port(node, server), player, hand)) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof DigitalNode node && node.permitted(server)
                && player.isShiftKeyDown() && (kind.endsWith("_bus") || kind.endsWith("_plane"))) {
            if (kind.startsWith("fluid_") && DigitalFluidAutomation.filter(stack).isBlank()) {
                server.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Select a fluid with its filled bucket. Sneak-use an empty hand to clear.")); return InteractionResult.SUCCESS;
            }
            node.filter(stack);
            server.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Transfer filter: ").append(stack.getHoverName()));
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == DigitalStorage.NODE ? (world, pos, s, be) -> ((DigitalNode) be).tick() : null;
    }
}
