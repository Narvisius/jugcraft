package io.github.jimbozoomer.jugcraft.powerline;

import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.CreativeEnergyCellBlock;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code /powerlines showcase} (operators only): builds every power line structure around you, wired into working
 * lines, with Creative Energy Cells at the boxes so the lines carry power (and their wires are live). For looking
 * at the models in game and for the client screenshot test. Lay it out on flat, open ground: it reaches about
 * 140 blocks west, 130 east and 310 north of where you stand.
 */
public final class PowerLineShowcase {
	private PowerLineShowcase() {
	}

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("powerlines").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("showcase").executes(context -> {
							var player = context.getSource().getPlayerOrException();
							int built = build(player.level(), player.blockPosition());
							context.getSource().sendSuccess(() -> Component.literal("Built " + built
									+ " power line structures: distribution line just north of you, monopoles 80 north, lattice towers 170 north,"
									+ " UHV and UHVDC 300 north, superconducting line 115 west"), false);
							return built;
						}))));
	}

	/** Builds the showcase around {@code origin}; returns how many structures were placed. */
	public static int build(ServerLevel level, BlockPos origin) {
		int x = origin.getX(), z = origin.getZ();
		int n = 0;
		// Distribution: pedestal - wooden pole - double-arm - spun concrete - pad-mount transformer.
		n += line(level, true, new int[][] {{x - 22, z - 24}, {x - 16, z - 24}, {x + 2, z - 24}, {x + 20, z - 24}, {x + 24, z - 20}},
				LineKind.SERVICE_PEDESTAL, LineKind.WOODEN_POLE, LineKind.DOUBLE_ARM_POLE, LineKind.CONCRETE_POLE, LineKind.PAD_MOUNT_TRANSFORMER);
		// Sub-transmission: pad-mount - the three monopoles - Unit Substation.
		n += line(level, true, new int[][] {{x - 56, z - 86}, {x - 50, z - 80}, {x - 15, z - 80}, {x + 20, z - 80}, {x + 50, z - 80}},
				LineKind.PAD_MOUNT_TRANSFORMER, LineKind.STEEL_MONOPOLE, LineKind.DOUBLE_CIRCUIT_MONOPOLE, LineKind.COMPACT_MONOPOLE,
				LineKind.UNIT_SUBSTATION);
		// Transmission: substation - the three lattice towers - substation.
		n += line(level, true, new int[][] {{x - 112, z - 170}, {x - 90, z - 170}, {x - 30, z - 170}, {x + 30, z - 170}, {x + 80, z - 170}},
				LineKind.UNIT_SUBSTATION, LineKind.LATTICE_TOWER, LineKind.RECONDUCTORED_LATTICE_TOWER, LineKind.MONITORED_LATTICE_TOWER,
				LineKind.UNIT_SUBSTATION);
		// The top tiers: two UHV towers, and converter - UHVDC towers - converter.
		n += line(level, false, new int[][] {{x - 140, z - 300}, {x - 60, z - 300}}, LineKind.UHV_TOWER, LineKind.UHV_TOWER);
		n += line(level, true, new int[][] {{x - 38, z - 300}, {x - 10, z - 300}, {x + 70, z - 300}, {x + 130, z - 300}},
				LineKind.CONVERTER_STATION, LineKind.UHVDC_TOWER, LineKind.UHVDC_TOWER, LineKind.CONVERTER_STATION);
		// Superconducting: substation (with nitrogen) - three pylons - substation.
		n += line(level, true, new int[][] {{x - 120, z - 75}, {x - 115, z - 55}, {x - 115, z - 35}, {x - 115, z - 15}, {x - 120, z + 5}},
				LineKind.UNIT_SUBSTATION, LineKind.SUPERCONDUCTING_PYLON, LineKind.SUPERCONDUCTING_PYLON, LineKind.SUPERCONDUCTING_PYLON,
				LineKind.UNIT_SUBSTATION);
		LineNetworks.invalidate(level);
		return n;
	}

	/** Places structures on the ground at each spot, wires each to the next, and powers the boxes at the ends. */
	private static int line(ServerLevel level, boolean powered, int[][] spots, LineKind... kinds) {
		BlockPos previous = null;
		int placed = 0;
		for (int i = 0; i < kinds.length; i++) {
			// Load the chunk first: the heightmap of an unloaded chunk reads as below the world.
			level.getChunk(net.minecraft.core.SectionPos.blockToSectionCoord(spots[i][0]), net.minecraft.core.SectionPos.blockToSectionCoord(spots[i][1]));
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spots[i][0], spots[i][1]);
			BlockPos anchor = LinePlacement.place(level, new BlockPos(spots[i][0], y, spots[i][1]), kinds[i], null);
			if (anchor == null) {
				previous = null;
				continue;
			}
			placed++;
			if (previous != null && LinePlacement.canJoin(level, previous, anchor) == null) {
				((LineAnchorBlockEntity) level.getBlockEntity(previous)).connect(anchor);
				((LineAnchorBlockEntity) level.getBlockEntity(anchor)).connect(previous);
			}
			if (kinds[i].isBox() && level.getBlockEntity(anchor) instanceof LineAnchorBlockEntity box) {
				if (powered) {
					level.setBlockAndUpdate(anchor.west(), CreativeEnergyCellBlock.BLOCK.defaultBlockState());
				}
				if (box.nitrogen != null && PetroFluids.NITROGEN != null) {
					box.nitrogen.fill(PetroFluids.NITROGEN.fluid(), LineAnchorBlockEntity.NITROGEN_TANK_MB);
				}
			}
			previous = anchor;
		}
		return placed;
	}
}
