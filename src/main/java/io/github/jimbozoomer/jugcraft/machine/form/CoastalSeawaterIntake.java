package io.github.jimbozoomer.jugcraft.machine.form;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

/** Bounded renewable shoreline sampling; no block removal, remote storage access or chunk loading. */
public final class CoastalSeawaterIntake {
    private static final Identifier ID = Jugcraft.id("coastal_seawater_intake");
    public static final int MILLIBUCKETS = 200;
    public static final int ENERGY = 320;

    private CoastalSeawaterIntake() {}

    public static boolean matches(MachineForm form) { return form.id().equals(ID); }

    /** Flat worlds report their bottom layer as sea level, where a two-deep intake cannot exist.
     * Use the normal Overworld reference for such creative test worlds; real generators keep their sea level. */
    public static int surfaceY(ServerLevel level) {
        int sea = level.getSeaLevel();
        return (sea <= level.getMinY()+1 ? 63 : sea)-1;
    }

    public static BlockPos source(FormMachineBlockEntity machine) {
        Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
        return machine.form().cellPos(machine.getBlockPos(), facing, machine.form().cellIndex(2,0,0))
                .relative(facing,2);
    }

    /** The intake's water is tested, not just the controller's biome. World height follows its sea-level setting. */
    public static boolean validSource(ServerLevel level, BlockPos center) {
        if (!level.dimension().equals(Level.OVERWORLD) || center.getY() != surfaceY(level)) return false;
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            BlockPos top = center.offset(x,0,z);
            if (!level.isLoaded(top) || level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    top.getX(),top.getZ()) > top.getY()+1) return false;
            var biome = level.getBiome(top);
            if (!biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_BEACH)) return false;
            for (int depth=0;depth<2;depth++) {
                var water = level.getFluidState(top.below(depth));
                if (!water.is(Fluids.WATER) || !water.isSource()) return false;
            }
        }
        if (level.getBiome(center).is(BiomeTags.IS_OCEAN)) return true;
        for (Direction direction : Direction.Plane.HORIZONTAL) for (int distance=4;distance<=12;distance+=4) {
            BlockPos nearby = center.relative(direction,distance);
            if (level.isLoaded(nearby) && level.getBiome(nearby).is(BiomeTags.IS_OCEAN)) return true;
        }
        return false;
    }

    /** One atomic paid stroke, called at most once per second by the shared controller. */
    public static MachineStatus pump(FormMachineBlockEntity machine, ServerLevel level) {
        if (!matches(machine.form())) throw new IllegalArgumentException("Not a coastal intake");
        if (!validSource(level,source(machine))) return MachineStatus.of(MachineLifecycle.WAITING_INPUT, MachineStatus.Reason.COASTAL_SOURCE);
        var output = machine.tanks().output(0);
        var seawater = PetroFluids.SEAWATER.source();
        if (output.millibuckets()>0 && !output.variant.isOf(seawater))
            return MachineStatus.fluid(MachineLifecycle.OUTPUT_BLOCKED,MachineStatus.Reason.OUTPUT_OTHER_FLUID,0,seawater,MILLIBUCKETS);
        if (output.capacityMb()-output.millibuckets()<MILLIBUCKETS)
            return MachineStatus.fluid(MachineLifecycle.OUTPUT_BLOCKED,MachineStatus.Reason.OUTPUT_FULL,0,seawater,MILLIBUCKETS);
        if (machine.energy().getAmount()<ENERGY)
            return new MachineStatus(MachineLifecycle.WAITING_ENERGY,MachineStatus.Reason.NO_ENERGY,-1,0,ENERGY);
        ((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getAmount()-ENERGY);
        output.fill(seawater,MILLIBUCKETS);
        machine.setChanged();
        return MachineStatus.PROCESSING;
    }
}
