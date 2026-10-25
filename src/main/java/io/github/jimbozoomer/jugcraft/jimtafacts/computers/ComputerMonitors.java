package io.github.jimbozoomer.jugcraft.jimtafacts.computers;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Owner-matched rectangular monitor walls, bounded to the original eight-by-six display size. */
public final class ComputerMonitors {
    public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 15);
    private ComputerMonitors() {}
    public static final class MonitorBlock extends ComputerBlock {
        public MonitorBlock(Properties properties, String kind) { super(properties, kind); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder); builder.add(CONNECTIONS);
        }
        @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,
                net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit) {
            if(kind.equals("monitor_advanced")&&!player.isShiftKeyDown()&&hit.getDirection()==state.getValue(FACING)) {
                if(player instanceof net.minecraft.server.level.ServerPlayer p&&level.getBlockEntity(pos) instanceof ComputerNode node
                        && io.github.jimbozoomer.jugcraft.concordance.RateGate.allow(p,"jimta_monitor_touch",4)) {
                    boolean delivered=node.touch(p,hit);
                    p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(delivered?"Monitor touch sent":"No connected permitted computer; sneak-use to open monitor."));
                }
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            return super.useWithoutItem(state,level,pos,player,hit);
        }
    }
    static void reform(ComputerNode start) {
        if (!(start.getLevel() instanceof ServerLevel level)) return;
        Direction facing = start.getBlockState().getValue(ComputerBlock.FACING), right = facing.getCounterClockWise();
        Set<BlockPos> seen = new HashSet<>(); ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start.getBlockPos()); queue.add(start.getBlockPos()); boolean oversized = false;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            for (Direction dir : new Direction[]{right, right.getOpposite(), Direction.UP, Direction.DOWN}) {
                BlockPos next = pos.relative(dir);
                if (seen.contains(next) || !level.isLoaded(next)) continue;
                if (level.getBlockEntity(next) instanceof ComputerNode candidate && compatible(start, candidate, facing)) {
                    if (seen.size() >= 48) { oversized = true; continue; }
                    seen.add(next); queue.addLast(next);
                }
            }
        }
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos pos : seen) {
            int x = pos.getX() * right.getStepX() + pos.getZ() * right.getStepZ();
            minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
        }
        int width = maxX - minX + 1, height = maxY - minY + 1;
        boolean rectangle = !oversized && width <= 8 && height <= 6 && seen.size() == width * height;
        int ownX = start.getBlockPos().getX() * right.getStepX() + start.getBlockPos().getZ() * right.getStepZ();
        BlockPos anchor = start.getBlockPos().relative(right, minX - ownX).above(maxY - start.getBlockPos().getY());
        for (BlockPos pos : seen) if (level.getBlockEntity(pos) instanceof ComputerNode node) {
            int x = pos.getX() * right.getStepX() + pos.getZ() * right.getStepZ() - minX, y = maxY - pos.getY();
            node.monitorLayout(rectangle ? anchor : pos, rectangle ? width : 1, rectangle ? height : 1,
                    rectangle ? x : 0, rectangle ? y : 0);
        }
    }
    private static boolean compatible(ComputerNode a, ComputerNode b, Direction facing) {
        return a.kind().equals(b.kind()) && a.getBlockState().getBlock() == b.getBlockState().getBlock()
                && b.getBlockState().getValue(ComputerBlock.FACING) == facing && a.sameOwner(b);
    }
}
