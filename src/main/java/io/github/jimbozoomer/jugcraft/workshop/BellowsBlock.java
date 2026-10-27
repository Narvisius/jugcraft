package io.github.jimbozoomer.jugcraft.workshop;

import io.github.jimbozoomer.jugcraft.kinetic.KineticNetworks;
import io.github.jimbozoomer.jugcraft.placement.StructurePermissions;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.jspecify.annotations.Nullable;

/** Grounded bellows: nozzle faces the lower furnace; the rear socket receives shaft work. */
public final class BellowsBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PUMP = IntegerProperty.create("pump", 0, 3);
    public BellowsBlock(Properties properties) {
        super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PUMP, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING,PUMP); }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext c) {
        Direction toward = c.getClickedFace().getAxis().isHorizontal() ? c.getClickedFace().getOpposite() : c.getHorizontalDirection();
        var state = defaultBlockState().setValue(FACING,toward);
        return canSurvive(state,c.getLevel(),c.getClickedPos()) ? state : null;
    }
    @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) { return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP); }
    @Override protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess ticks, BlockPos p, Direction d, BlockPos n, BlockState ns, RandomSource r) {
        return d==Direction.DOWN && !canSurvive(s,l,p) ? Blocks.AIR.defaultBlockState() : super.updateShape(s,l,ticks,p,d,n,ns,r);
    }
    @Override protected void neighborChanged(BlockState s, Level l, BlockPos p, Block b, @Nullable Orientation o, boolean moved) { KineticNetworks.invalidate(l); }
    @Override protected void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moved) { KineticNetworks.invalidate(l); }
    @Override protected void affectNeighborsAfterRemoval(BlockState s, ServerLevel l, BlockPos p, boolean moved) { KineticNetworks.invalidate(l); }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new BellowsBlockEntity(p,s); }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l instanceof ServerLevel ? createTickerHelper(t,JugcraftWorkshop.BELLOWS_ENTITY,(w,p,b,e)->e.tick((ServerLevel)w)) : null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        if (!StructurePermissions.mayChange(l,p,player) || l instanceof ServerLevel server && !Authority.mayChange(server,player,p)) return InteractionResult.FAIL;
        if (l.getBlockEntity(p) instanceof BellowsBlockEntity be && be.furnace() instanceof WorkshopBlockEntity furnace) {
            if (l instanceof ServerLevel) {
                if (!furnace.mayOperate(player)) return InteractionResult.FAIL;
                player.openMenu(furnace);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(1,0,1,15,13,15); }
}
