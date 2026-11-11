package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * The medium plant's water works working together (towers and pump houses build, 19 October 2026: "everything
 * works"): the cooling towers' basins, the lake intake's make-up and service water pumps, the circulating water pump
 * house.
 * <ul>
 * <li>A tower takes make-up from the nearest intake within {@link #REACH} blocks (its make-up pumps delivering); with
 * no intake loaded (a plant built before the intake was, or the intake far off and unloaded), make-up comes from the
 * lake as it always did.</li>
 * <li>The circulating water pump house's forebay is its towers' basins: low, its pumps lose suction and trip.</li>
 * <li>Service water: each diesel generator's jacket water cooler and the component cooling heat exchangers are cooled
 * by a service water train of the nearest intake (diesel A by train 1, B by train 2). A diesel running without its
 * train's water heats up and trips on high jacket water temperature (its 86G lockout); the boards' component cooling
 * alarms come in.</li>
 * </ul>
 */
public final class WaterWorks {
	/** How far a tower or a diesel generator looks for its intake (blocks). */
	public static final int REACH = 600;
	/** How far a circ pump house looks for the towers its forebay is fed from (blocks). */
	public static final int TOWER_REACH = 400;
	/**
	 * A diesel's jacket water above its normal running temperature with no service water: how fast it climbs and falls
	 * back (degrees C a second), and where it trips (205 degrees F: 17 degrees C over the normal 79 C).
	 */
	public static final float JW_RISE = 0.25F, JW_FALL = 0.5F, JW_TRIP = 17F;

	private WaterWorks() {
	}

	/** The nearest intake within {@link #REACH} whose controller is loaded, or null. */
	public static @Nullable WaterWorksBlockEntity intake(ServerLevel level, BlockPos near) {
		WaterWorksBlockEntity best = null;
		double bestD = (double) REACH * REACH;
		for (SiteRegistry.Placement p : SiteRegistry.placements(level, WaterWorksBlock.Works.INTAKE.site)) {
			double d = p.anchor().distSqr(near);
			if (d <= bestD && SiteRegistry.anchorOf(level, p) instanceof WaterWorksBlockEntity works && works.intake()) {
				best = works;
				bestD = d;
			}
		}
		return best;
	}

	/** Whether make-up water reaches a tower's basin now (its MU-1 aside). */
	public static boolean makeupAvailable(ServerLevel level, BlockPos tower) {
		WaterWorksBlockEntity intake = intake(level, tower);
		return intake == null || intake.makeup();
	}

	/** Whether service water train t (0 or 1) reaches a consumer near {@code at} (true with no intake: the old lake). */
	public static boolean serviceWater(ServerLevel level, BlockPos at, int train) {
		WaterWorksBlockEntity intake = intake(level, at);
		return intake == null || intake.serviceWater(train);
	}

	/** Either train's service water (the circ pumps' bearing cooling). */
	public static boolean serviceWaterAny(ServerLevel level, BlockPos at) {
		WaterWorksBlockEntity intake = intake(level, at);
		return intake == null || intake.serviceWater(0) || intake.serviceWater(1);
	}

	/**
	 * The service water as the boards show it: -1 with no intake loaded near (they keep their old readings), else bit 0
	 * train 1 (A) flowing, bit 1 train 2 (B), bit 2 make-up, bits 8 to 13 the intake's pumps running.
	 */
	public static int boardState(ServerLevel level, BlockPos near) {
		WaterWorksBlockEntity intake = intake(level, near);
		if (intake == null) return -1;
		int s = (intake.serviceWater(0) ? 1 : 0) | (intake.serviceWater(1) ? 2 : 0) | (intake.makeup() ? 4 : 0);
		for (int p = 0; p < 6; p++) {
			if (intake.running(p)) s |= 1 << 8 + p;
		}
		return s;
	}

	/** A circ pump house's forebay: the highest basin of the medium towers near it (1 if there are none). */
	public static float forebay(ServerLevel level, BlockPos house) {
		float best = -1;
		for (SiteRegistry.Placement p : SiteRegistry.placements(level, "ct_medium")) {
			if (p.anchor().distSqr(house) <= (double) TOWER_REACH * TOWER_REACH
					&& SiteRegistry.anchorOf(level, p) instanceof CoolingTowerBlockEntity tower) {
				best = Math.max(best, tower.basin());
			}
		}
		return best < 0 ? 1F : best;
	}

	/** The circ pumps delivering from the pump houses a condenser's conduit passes (their controllers). */
	public static int circPumps(ServerLevel level, List<BlockPos> houses) {
		int n = 0;
		for (BlockPos at : houses) {
			if (level.isLoaded(at) && level.getBlockEntity(at) instanceof WaterWorksBlockEntity works && !works.intake()) {
				n += works.pumpsDelivering();
			}
		}
		return n;
	}

	/** A diesel generator's service water train: 0 for the western of its pair (A, as the boards name them), else 1. */
	public static int edgTrain(ServerLevel level, BlockPos edg) {
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		BlockPos nearest = null;
		double best = 80.0 * 80.0;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : registry.anchors().entrySet()) {
			if (e.getValue().kind() != EquipmentKind.EMERGENCY_DIESEL_GENERATOR || e.getKey().equals(edg)) continue;
			double d = e.getKey().distSqr(edg);
			if (d < best) {
				best = d;
				nearest = e.getKey();
			}
		}
		if (nearest == null) return 0;
		return edg.getX() < nearest.getX() || edg.getX() == nearest.getX() && edg.getZ() < nearest.getZ() ? 0 : 1;
	}

	/**
	 * Each second (spread by position): a running diesel's jacket water climbs while its service water train is lost
	 * and falls back with it (or stopped); at {@link #JW_TRIP} over normal the high jacket water temperature trip
	 * opens its breaker and stops it on its 86G lockout.
	 */
	public static void tickDiesel(ServerLevel level, EquipmentBlockEntity edg, long now) {
		if (Math.floorMod(now + edg.getBlockPos().asLong() + 13, 20) != 0) {
			return;
		}
		float was = edg.jwExcess;
		boolean cooled = !edg.running || serviceWater(level, edg.getBlockPos(), edgTrain(level, edg.getBlockPos()));
		edg.jwExcess = cooled ? Math.max(0, edg.jwExcess - JW_FALL) : edg.jwExcess + JW_RISE;
		if (edg.running && edg.jwExcess >= JW_TRIP) {
			DieselPanels.tripLockout(level, edg.getBlockPos(), edg, null, Component.empty());
		}
		if (Math.round(was * 2) != Math.round(edg.jwExcess * 2)) {
			edg.changed();
		}
	}

	/** The controllers of the circ pump houses touching a set of conduit cells. */
	public static List<BlockPos> housesAt(ServerLevel level, Iterable<BlockPos> cells) {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos c : cells) {
			if (level.getBlockState(c).getBlock() instanceof WaterWorksBlock
					&& level.getBlockState(c).getValue(WaterWorksBlock.WORKS) == WaterWorksBlock.Works.CIRC && !out.contains(c)) {
				out.add(c.immutable());
			}
		}
		return out;
	}
}
