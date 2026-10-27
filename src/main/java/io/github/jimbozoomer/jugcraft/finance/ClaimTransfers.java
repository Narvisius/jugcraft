package io.github.jimbozoomer.jugcraft.finance;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Unattended machinery has no player permission; keep its transfers inside one owner's land. */
public final class ClaimTransfers {
    private ClaimTransfers() {}
    public static boolean allowed(Level level, BlockPos from, BlockPos to) {
        return !io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation.reserved(level,from)
                &&!io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation.reserved(level,to)
                &&level.hasChunkAt(from) && level.hasChunkAt(to) && !LandRegistry.crossesBoundary(level, from, to)
                && intactEndpoint(level, from) && intactEndpoint(level, to);
    }
    private static boolean intactEndpoint(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlock machine) {
            var master = machine.masterPos(pos, state);
            return level.hasChunkAt(master) && !LandRegistry.crossesBoundary(level, pos, master);
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
            var other = pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
            return level.hasChunkAt(other) && !LandRegistry.crossesBoundary(level, pos, other);
        }
        return true;
    }
}
