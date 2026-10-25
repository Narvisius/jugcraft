package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Original compact industrial housings using the shared JE, recipes, menus and automation. */
public final class JimtaIndustryMachine extends MachineBlock {
    public JimtaIndustryMachine(Properties properties, MachineKind kind) { super(properties, kind); }
    @Override public Footprint footprint(BlockState state) { return Footprint.SINGLE; }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        if (kind() == MachineKind.CIRCUIT_ASSEMBLER || state.is(JimtaIndustry.MACHINES.get("jimta_advanced_crafting_table"))) return net.minecraft.world.level.block.Block.box(0,0,0,16,9,16);
        return super.getShape(state,level,pos,context);
    }
    // Original machines expose their housing on each face, rather than an offset port on a large machine.
    // Battery direction is still enforced by MachineBlockEntity.energyFor.
    @Override public boolean acceptsPower(BlockState state, @Nullable Direction side) { return kind().usesPower(); }
}
