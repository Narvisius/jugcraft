package io.github.jimbozoomer.jugcraft.workshop;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import io.github.jimbozoomer.jugcraft.placement.StructurePermissions;
import java.util.ArrayList;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
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

/** One inventory and one tick per station. Other parts only forward interaction and hoppers. */
public final class WorkshopBlock extends BaseEntityBlock implements WorldlyContainerHolder, StructureGhost.Source {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 1);
    public static final BooleanProperty UPGRADED = BooleanProperty.create("upgraded");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final ThreadLocal<Boolean> CHECKING_BREAK = ThreadLocal.withInitial(() -> false);
    public final int station;
    public WorkshopBlock(Properties properties, int station) {
        super(properties); this.station = station;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0).setValue(LIT, false).setValue(UPGRADED, false));
    }
    public Footprint footprint() { return station == 0 ? Footprint.cuboid(2, 1, 1) : station == 2 ? Footprint.tall(2) : Footprint.SINGLE; }
    public BlockPos master(BlockPos pos, BlockState state) { return footprint().masterPos(pos, state.getValue(FACING), Math.min(state.getValue(PART), footprint().size() - 1)); }
    public boolean complete(Level level, BlockPos master, BlockState state) {
        for (int i = 0; i < footprint().size(); i++) {
            BlockPos p = footprint().partPos(master, state.getValue(FACING), i);
            if (!level.isLoaded(p)) return false;
            var found = level.getBlockState(p);
            if (!found.is(this) || found.getValue(PART) != i || found.getValue(FACING) != state.getValue(FACING)) return false;
        }
        return true;
    }
    public boolean mayUse(Level level, BlockPos master, BlockState state, Player player) {
        for (int i = 0; i < footprint().size(); i++) {
            BlockPos p = footprint().partPos(master, state.getValue(FACING), i);
            if (!StructurePermissions.mayChange(level, p, player)
                || level instanceof ServerLevel s && !Authority.mayChange(s, player, p)) return false;
        }
        return true;
    }
    public static void registerProtection() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
            if (!(state.getBlock() instanceof WorkshopBlock block) || CHECKING_BREAK.get()) return true;
            CHECKING_BREAK.set(true);
            try { return block.mayUse(level, block.master(pos, state), state, player); }
            finally { CHECKING_BREAK.set(false); }
        });
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        Player player = context.getPlayer();
        if (player == null || !mayUse(context.getLevel(), context.getClickedPos(), state, player)) return null;
        for (int i = 0; i < footprint().size(); i++) {
            BlockPos p = footprint().partPos(context.getClickedPos(), state.getValue(FACING), i);
            if (!context.getLevel().getBlockState(p).canBeReplaced(BlockPlaceContext.at(context, p, Direction.UP))) return null;
        }
        return state;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide()) for (int i = 1; i < footprint().size(); i++)
            level.setBlock(footprint().partPos(pos, state.getValue(FACING), i), state.setValue(PART, i), UPDATE_ALL);
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new WorkshopBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel && state.getValue(PART) == 0
            ? createTickerHelper(type, JugcraftWorkshop.ENTITY, (l, p, s, be) -> be.tick((ServerLevel) l)) : null;
    }
    @Override public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(master(pos, state)) instanceof WorkshopBlockEntity be ? be : null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos master = master(pos, state);
        if (!mayUse(level, master, state, player)) return InteractionResult.FAIL;
        if (!level.isClientSide() && level.getBlockEntity(master) instanceof WorkshopBlockEntity be) player.openMenu(be);
        return InteractionResult.SUCCESS;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (level.getBlockState(pos).is(this)) return;
        BlockPos master = master(pos, state);
        for (int i = 0; i < footprint().size(); i++) {
            BlockPos other = footprint().partPos(master, state.getValue(FACING), i);
            if (other.equals(pos)) continue;
            // A two-cell station can straddle a chunk border. Load its already placed neighbour for teardown,
            // otherwise breaking at a boundary could leave a second item-producing orphan behind.
            level.getChunkAt(other);
            var found = level.getBlockState(other);
            if (found.is(this) && found.getValue(PART) == i && found.getValue(FACING) == state.getValue(FACING))
                level.removeBlock(other, false);
        }
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (station == 0) return Shapes.or(Block.box(0, 12, 1, 16, 16, 15), Block.box(2, 0, 2, 5, 12, 5), Block.box(11, 0, 11, 14, 12, 14));
        return station == 2 && state.getValue(PART) == 1 ? Block.box(2, 0, 2, 14, 12, 14) : Shapes.block();
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, PART, LIT, UPGRADED); }
    @Override public StructureGhost ghost(BlockPlaceContext context) {
        var state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        var cells = new ArrayList<StructureGhost.Cell>(); var blocked = new ArrayList<BlockPos>();
        for (int i = 0; i < footprint().size(); i++) {
            BlockPos p = footprint().partPos(context.getClickedPos(), state.getValue(FACING), i);
            cells.add(new StructureGhost.Cell(p, state.setValue(PART, i)));
            if (!context.getLevel().isInWorldBounds(p) || !context.getLevel().getBlockState(p).canBeReplaced(BlockPlaceContext.at(context, p, Direction.UP))) blocked.add(p);
        }
        return new StructureGhost(cells, blocked);
    }
}
