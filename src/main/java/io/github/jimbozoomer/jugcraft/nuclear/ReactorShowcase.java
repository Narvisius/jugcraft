package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Builds a complete, fuelled, running stage-1 reactor unit on flat ground (for operators and the game tests):
 * the vessel with four steam generators and coolant pumps on a Reactor Coolant Pipe header, a pressurizer, steam
 * lines up to a turbine-generator with its feedwater pump, the condenser drawing from a pond through a circulating
 * water conduit, the main transformer, and a console linked to the vessel. North is "away" for every piece.
 */
public final class ReactorShowcase {
	private ReactorShowcase() {
	}

	/** Builds the unit with {@code origin} as its south-west reference on the ground. Returns the vessel's anchor. */
	public static BlockPos build(ServerLevel level, BlockPos origin) {
		for (int x = -24; x <= 40; x += 16) {
			for (int z = -56; z <= 8; z += 16) {
				level.getChunk(SectionPos.blockToSectionCoord(origin.getX() + x), SectionPos.blockToSectionCoord(origin.getZ() + z));
			}
		}
		// Clear the site.
		for (BlockPos p : BlockPos.betweenClosed(origin.offset(-22, 0, -50), origin.offset(38, 24, 4))) {
			if (!level.getBlockState(p).isAir()) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		Direction n = Direction.NORTH;
		BlockPos vessel = origin.offset(0, 0, -10);
		place(level, vessel, EquipmentKind.REACTOR_VESSEL, n);
		int[] sgX = {-8, -16, 8, 16};
		int[] pumpX = {-7, -15, 9, 17};
		for (int i = 0; i < 4; i++) {
			place(level, origin.offset(sgX[i], 0, -14), EquipmentKind.STEAM_GENERATOR, n);
			place(level, origin.offset(pumpX[i], 0, -10), EquipmentKind.COOLANT_PUMP, n);
			// A stub from the header into the steam generator's lower shell.
			pipe(level, origin.offset(sgX[i] + 3, 1, -14), PipeKind.COOLANT);
		}
		place(level, origin.offset(23, 0, -14), EquipmentKind.PRESSURIZER, n);
		// The coolant header either side of the vessel, and the steam header above it.
		for (int x = -16; x <= -1; x++) {
			pipe(level, origin.offset(x, 1, -13), PipeKind.COOLANT);
			pipe(level, origin.offset(x, 12, -13), PipeKind.STEAM);
		}
		for (int x = 7; x <= 24; x++) {
			pipe(level, origin.offset(x, 1, -13), PipeKind.COOLANT);
			if (x <= 22) {
				pipe(level, origin.offset(x, 12, -13), PipeKind.STEAM);
			}
		}
		// Steam lines north to the turbine.
		for (int x : new int[] {-1, 7}) {
			for (int z = -14; z >= -29; z--) {
				pipe(level, origin.offset(x, 12, z), PipeKind.STEAM);
			}
			for (int y = 1; y < 12; y++) {
				pipe(level, origin.offset(x, y, -29), PipeKind.STEAM);
			}
		}
		place(level, origin.offset(-4, 0, -28), EquipmentKind.FEEDWATER_PUMP, n);
		place(level, origin.offset(-20, 0, -40), EquipmentKind.TURBINE_GENERATOR, Direction.EAST);
		place(level, origin.offset(2, 0, -41), EquipmentKind.CONDENSER, n);
		place(level, origin.offset(30, 0, -38), EquipmentKind.MAIN_TRANSFORMER, n);
		// The pond and the conduit dipping into it.
		for (BlockPos p : BlockPos.betweenClosed(origin.offset(16, -1, -45), origin.offset(20, -1, -41))) {
			level.setBlock(p, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
		}
		for (int x = 14; x <= 20; x++) {
			pipe(level, origin.offset(x, 0, -43), PipeKind.WATER);
		}
		BlockPos console = origin.offset(-6, 0, -2);
		place(level, console, EquipmentKind.CONTROL_CONSOLE, n);
		if (level.getBlockEntity(console) instanceof EquipmentBlockEntity c) {
			c.linked = vessel;
			c.changed();
		}
		// Fuel, water, and the rods partly out.
		if (level.getBlockEntity(vessel) instanceof EquipmentBlockEntity v) {
			v.core.refuelOpen = true;
			v.loadAssemblies(ReactorCore.CORE_SIZE);
			v.core.refuelOpen = false;
			v.core.rodTarget = 40;
			v.core.rods = 40;
			v.core.fission = 0.6;
			v.core.decay = ReactorCore.DECAY_SHARE * 0.6;
			v.core.temperature = 300;
			v.changed();
		}
		if (level.getBlockEntity(origin.offset(2, 0, -41)) instanceof EquipmentBlockEntity c && c.water != null) {
			c.water.fill(Fluids.WATER, EquipmentBlockEntity.CONDENSER_TANK);
		}
		return vessel;
	}

	private static void place(ServerLevel level, BlockPos anchor, EquipmentKind kind, Direction facing) {
		if (!EquipmentPlacement.place(level, anchor, kind, facing, null)) {
			throw new IllegalStateException("no room for " + kind.id + " at " + anchor);
		}
	}

	private static void pipe(ServerLevel level, BlockPos pos, PipeKind kind) {
		if (!level.getBlockState(pos).canBeReplaced()) {
			throw new IllegalStateException("a " + kind.id + " would replace " + level.getBlockState(pos) + " at " + pos);
		}
		BlockState state = JugcraftNuclear.PIPES.get(kind).defaultBlockState();
		level.setBlock(pos, state, Block.UPDATE_ALL);
		BlockState placed = level.getBlockState(pos);
		if (placed.getBlock() instanceof NuclearPipeBlock pipe) {
			level.setBlock(pos, pipe.refresh(level, pos, placed), Block.UPDATE_ALL);
		}
	}
}
