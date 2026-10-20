package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Banyan canopy anchors aerial roots underneath; other leaf faces retain ordinary support rules. */
public final class BanyanLeavesBlock extends TintedParticleLeavesBlock {
    private static final VoxelShape ROOT_SUPPORT = Shapes.box(0, 0, 0, 1, 1.0 / 16, 1);
    public BanyanLeavesBlock(Properties properties) { super(0.01F, properties); }
    @Override protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return ROOT_SUPPORT;
    }
}
