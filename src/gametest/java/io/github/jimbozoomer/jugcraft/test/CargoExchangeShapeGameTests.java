package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

/** Checks the sealed cargo housing and its attached socket in every rotation. */
public class CargoExchangeShapeGameTests {
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 20)
	public void cargoHousingRotatesWithSolidFrontSidesAndInterior(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		var cargo = ExchangeStations.CARGO;
		BlockPos master = h.absolutePos(new BlockPos(20, 2, 20));
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			for (int part = 0; part < cargo.footprint().size(); part++) {
				var state = cargo.defaultBlockState().setValue(ExchangeStationBlock.LARGE, false).setValue(ExchangeStationBlock.PART, part).setValue(ExchangeStationBlock.FACING, facing);
				BlockPos at = cargo.footprint().partPos(master, facing, part);
				for (var box : state.getCollisionShape(h.getLevel(), at).toAabbs())
					h.assertTrue(box.minX >= 0 && box.minY >= 0 && box.minZ >= 0 && box.maxX <= 1 && box.maxY <= 1 && box.maxZ <= 1,
						"cargo collisions stay inside each rotated native part");
			}
			point(h, master, facing, 11, 37, 10, true, "integrated operator bank is solid");
			point(h, master, facing, 49, 45, 35, true, "enclosed roof is solid");
			point(h, master, facing, 3, 30, 35, true, "left enclosure is solid");
			point(h, master, facing, 77, 30, 35, true, "right enclosure is solid");
			point(h, master, facing, 49, 30, 61, true, "rear enclosure is solid");
			point(h, master, facing, 49, 30, 2, true, "front bulkhead is closed");
			point(h, master, facing, 40, 24, 1, true, "outward union has a solid backing");
			for (double y : new double[] {10, 18, 30, 41})
				point(h, master, facing, 49, y, 35, true, "cargo interior is closed, without a transfer tunnel");
			point(h, master, facing, 1, 30, 35, false, "air beside the housing stays clear");
			point(h, master, facing, 49, 47, 35, false, "air above the housing stays clear");
		}
		h.succeed();
	}
	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 20)
	public void nineWideMachinesKeepNativeCollisionInsideAll270RotatedCells(GameTestHelper h) {
		if (TestFocus.skip("tower")) { h.succeed(); return; }
		BlockPos master = h.absolutePos(new BlockPos(20, 2, 20));
		for (var block : new ExchangeStationBlock[] {ExchangeStations.CARGO, ExchangeStations.ENERGY}) for (Direction facing : Direction.Plane.HORIZONTAL) {
			var masterState = block.defaultBlockState().setValue(ExchangeStationBlock.LARGE, true).setValue(ExchangeStationBlock.FACING, facing);
			var footprint = block.footprint(masterState);
			h.assertTrue(footprint.size() == 270 && block.portPart(masterState) == 58, "large native slicing is270 cells with the centered part58 socket");
			for (int part = 0; part < footprint.size(); part++) {
				var state = masterState.setValue(ExchangeStationBlock.PART, part);
				BlockPos at = footprint.partPos(master, facing, part);
				var collision = state.getCollisionShape(h.getLevel(), at);
				h.assertTrue(state.getShape(h.getLevel(), at).equals(collision), "selection and physical collision identify the same large part");
				for (var box : collision.toAabbs()) h.assertTrue(box.minX >= -1E-6 && box.minY >= -1E-6 && box.minZ >= -1E-6
						&& box.maxX <= 1 + 1E-6 && box.maxY <= 1 + 1E-6 && box.maxZ <= 1 + 1E-6, "large native collision stays within its rotated occupied cell");
			}
			largePoint(h, block, master, facing, 72, 24, 1, true, "native union is solid at the centered front socket");
			// Keep solid probes inside their cell: rotated max-face points are excluded by AABB.contains.
			largePoint(h, block, master, facing, 72.5, 2, 48.5, true, "continuous concrete foundation is solid");
			largePoint(h, block, master, facing, 143, 40, 40, false, "air beside the housing stays clear");
			largePoint(h, block, master, facing, 72, 79, 40, false, "air above the physical housing stays clear");
			if (block.energy) {
				largePoint(h, block, master, facing, 72, 40, 12, true, "large instrument cabinets are solid");
				largePoint(h, block, master, facing, 72, 40, 70, true, "rear transformer body is solid");
				largePoint(h, block, master, facing, 18, 40, 70, true, "left cooling bank is solid");
				largePoint(h, block, master, facing, 125, 40, 70, true, "right cooling bank is solid");
				largePoint(h, block, master, facing, 32, 40, 70, false, "native service gap between cooling bank and transformer remains clear");
			} else {
				largePoint(h, block, master, facing, 72, 40, 50, true, "large cargo interior is sealed rather than an exposed conveyor");
				largePoint(h, block, master, facing, 3, 40, 50, true, "left charcoal enclosure is solid");
				largePoint(h, block, master, facing, 141, 40, 50, true, "right charcoal enclosure is solid");
				largePoint(h, block, master, facing, 72, 40, 94, true, "rear service enclosure is solid");
			}
		}
		h.succeed();
	}
	private static void largePoint(GameTestHelper h, ExchangeStationBlock block, BlockPos master, Direction facing,
			double x, double y, double z, boolean expected, String why) {
		var base = block.defaultBlockState().setValue(ExchangeStationBlock.LARGE, true).setValue(ExchangeStationBlock.FACING, facing);
		var footprint = block.footprint(base);
		int cx = (int) x / 16, cy = (int) y / 16, cz = (int) z / 16;
		int part = footprint.offsets().indexOf(new Vec3i(cx, cy, cz));
		double lx = x - cx * 16, ly = y - cy * 16, lz = z - cz * 16;
		int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
		for (int i = 0; i < turns; i++) { double nx = 16 - lz; lz = lx; lx = nx; }
		Vec3 p = new Vec3(lx / 16, ly / 16, lz / 16);
		var state = base.setValue(ExchangeStationBlock.PART, part);
		boolean solid = state.getCollisionShape(h.getLevel(), footprint.partPos(master, facing, part)).toAabbs().stream().anyMatch(box -> box.contains(p));
		h.assertTrue(solid == expected, why + " facing " + facing);
	}
	private static void point(GameTestHelper h, BlockPos master, Direction facing, double x, double y, double z, boolean expected, String why) {
		var cargo = ExchangeStations.CARGO;
		int cx = (int) x / 16, cy = (int) y / 16, cz = (int) z / 16;
		int part = cargo.footprint().offsets().indexOf(new Vec3i(cx, cy, cz));
		double lx = x - cx * 16, ly = y - cy * 16, lz = z - cz * 16;
		int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
		for (int i = 0; i < turns; i++) { double nx = 16 - lz; lz = lx; lx = nx; }
		Vec3 p = new Vec3(lx / 16, ly / 16, lz / 16);
		var state = cargo.defaultBlockState().setValue(ExchangeStationBlock.LARGE, false).setValue(ExchangeStationBlock.PART, part).setValue(ExchangeStationBlock.FACING, facing);
		boolean solid = state.getCollisionShape(h.getLevel(), cargo.footprint().partPos(master, facing, part)).toAabbs().stream().anyMatch(box -> box.contains(p));
		h.assertTrue(solid == expected, why + " facing " + facing);
	}
}
