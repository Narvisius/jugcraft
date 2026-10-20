package io.github.jimbozoomer.jugcraft.vehiclebay;

import java.util.ArrayList;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import org.jspecify.annotations.Nullable;

/** Uses the shared large-machine lifecycle; only the deck, four posts and top rails occupy cells. */
public class VehicleBayBlock extends LargeMachineBlock {
    public final int bayHeight;
    private final Footprint footprint;
    public VehicleBayBlock(Properties properties, int height) {
        super(properties, MachineKind.CIRCUIT_ASSEMBLER);
        bayHeight = height;
        var cells = new ArrayList<Vec3i>();
        for (int z = 0; z < 6; z++) for (int x = 0; x < 6; x++) cells.add(new Vec3i(-x, 0, z));
        for (int y = 1; y < height; y++) for (int z : new int[]{0, 5}) for (int x : new int[]{0, 5}) cells.add(new Vec3i(-x, y, z));
        for (int z = 1; z < 5; z++) for (int x : new int[]{0, 5}) cells.add(new Vec3i(-x, height - 1, z));
        footprint = new Footprint(java.util.List.copyOf(cells));
    }
    @Override public Footprint footprint(BlockState state) { return footprint; }
    @Override public boolean acceptsPower(BlockState state, @Nullable Direction side) {
        return part(state) == 0 && (side == null || side == Footprint.rotate(Direction.EAST, state.getValue(FACING)));
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        if (state == null) return null;
        var level = context.getLevel(); var origin = context.getClickedPos(); var facing = state.getValue(FACING);
        for (int y = 0; y < bayHeight; y++) for (int z = 0; z < 6; z++) for (int x = 0; x < 6; x++) {
            var pos = origin.offset(Footprint.rotate(new Vec3i(-x, y, z), facing));
            boolean blocked = level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.isLoaded(pos) || !level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context, pos, Direction.UP));
            if (level instanceof ServerLevel server && !Authority.mayChange(server, context.getPlayer(), pos)) blocked = true;
            if (blocked) {
                if (context.getPlayer() != null) context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.bay.blocked", pos.getX(), pos.getY(), pos.getZ()));
                return null;
            }
        }
        return state;
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = part(state);
        if (part == 0) return Block.box(0, 0, 0, 16, 16, 16);
        if (part < 36) return Block.box(0, 0, 0, 16, 3, 16);
        if (part >= footprint.size()) return Shapes.empty();
        var offset = footprint.offsets().get(part);
        if (offset.getZ() > 0 && offset.getZ() < 5) return Block.box(0, 12, 0, 16, 16, 16);
        return Block.box(3, 0, 3, 13, 16, 13);
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return part(state) == 0 ? new VehicleBayBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && part(state) == 0 ? createTickerHelper(type, VehicleBays.ENTITY,
            (world, pos, block, bay) -> bay.tick()) : null;
    }
    /** No furnace effects; the animated gantry and welding lights show powered work. */
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) { }
}
