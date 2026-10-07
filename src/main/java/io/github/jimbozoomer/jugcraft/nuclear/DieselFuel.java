package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/**
 * The emergency diesel generator's fuel (round 14, Justin: "consuming diesel fuel at a realistic rate"). A real 7 MW
 * emergency diesel burns about 0.25 litres a kWh at full load (about 1,690 litres an hour at 6,800 kW) and about 110
 * litres an hour running light; here a millibucket is a litre and an hour is a Minecraft hour (1,000 ticks, as on the
 * engine panel's hour meter). It burns from its day tank: the nearest formed storage tank holding diesel within
 * {@link #REACH} blocks (each generator room's day tank in the pre-made plant, which the fuel oil storage tank outside
 * keeps topped up through its fill line), drawn through the tank's outlet (shut the valve and the engine starves).
 * With no fuel it won't start, and running dry stops it.
 *
 * <p>The load follows the emergency start's load sequence, the same as the panels show (client/EdgTelemetry.java):
 * breaker closed {@link #BREAKER} s after the start, then a block of safety loads every {@link #STEP} s.
 */
public final class DieselFuel {
	public static final double BREAKER = 5, STEP = 5;
	public static final double[] STEP_KW = {300, 1500, 1100, 900, 1000, 900, 700, 400};
	/** Fuel: litres (mB) an hour running light, and per kW of load. */
	public static final double IDLE_LPH = 110, LPH_PER_KW = 0.232;
	/** Ticks in an hour (a Minecraft hour: the hour meter's too). */
	public static final int TICKS_PER_HOUR = 1000;
	/** How far (blocks) from the generator's anchor its day tank may stand. */
	public static final int REACH = 24;
	/** The day tank's low level alarm (share of the tank). */
	public static final double LOW = 0.25;

	private DieselFuel() {
	}

	/** The load (kW) {@code s} seconds after the start, as the sequencer adds it (each block coming on in about a second). */
	public static double kw(double s) {
		double p = 0;
		for (int k = 0; k < STEP_KW.length; k++) {
			double dt = s - (BREAKER + STEP * k);
			if (dt < 0) {
				break;
			}
			p += STEP_KW[k] * (1 - Math.exp(-dt / 0.6));
		}
		return p;
	}

	/** Litres (mB) an hour burned {@code s} seconds after the start (nothing while it is still cranking on air). */
	public static double litresPerHour(double s) {
		return s < 1.5 ? 0 : IDLE_LPH + LPH_PER_KW * kw(s);
	}

	public static FluidVariant diesel() {
		return FluidVariant.of(PetroFluids.DIESEL.source());
	}

	/** The generator's day tank (a formed tank's outlet), found again when it is gone, or null. */
	public static @Nullable StorageTankPortBlockEntity dayTank(ServerLevel level, EquipmentBlockEntity edg) {
		if (edg.dayTank != null && level.isLoaded(edg.dayTank) && level.getBlockEntity(edg.dayTank) instanceof StorageTankPortBlockEntity tank
				&& tank.tank() == tank && tank.formed() && holdsDiesel(tank)) {
			return tank;
		}
		edg.dayTank = null;
		BlockPos at = edg.getBlockPos();
		StorageTankPortBlockEntity best = null;
		int r = (REACH >> 4) + 1;
		for (int cx = (at.getX() >> 4) - r; cx <= (at.getX() >> 4) + r; cx++) {
			for (int cz = (at.getZ() >> 4) - r; cz <= (at.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof StorageTankPortBlockEntity tank && tank.tank() == tank && tank.formed() && holdsDiesel(tank)
							&& tank.getBlockPos().distSqr(at) <= (double) REACH * REACH
							&& (best == null || tank.getBlockPos().distSqr(at) < best.getBlockPos().distSqr(at))) {
						best = tank;
					}
				}
			}
		}
		if (best != null) {
			edg.dayTank = best.getBlockPos().immutable();
		}
		return best;
	}

	/** A tank with diesel in it, or an empty one meant for it (a day tank). */
	private static boolean holdsDiesel(StorageTankPortBlockEntity tank) {
		if (!tank.fluid.isResourceBlank() && tank.fluid.amount > 0) {
			return tank.fluid.variant.equals(diesel());
		}
		return tank.getBlockState().getBlock() instanceof StorageTankOutletBlock
				&& tank.getBlockState().getValue(StorageTankOutletBlock.SERVICE).diesel();
	}

	/** Whether the generator has fuel to start on. */
	public static boolean hasFuel(ServerLevel level, EquipmentBlockEntity edg) {
		StorageTankPortBlockEntity tank = dayTank(level, edg);
		return tank != null && tank.fluid.amount > 0 && tank.fluid.variant.equals(diesel());
	}

	/**
	 * Every tick: a running generator burns its fuel (taken from the day tank once a second); run dry, it stops. The
	 * day tank's level goes to the panels (its low level alarm).
	 */
	public static void tick(ServerLevel level, EquipmentBlockEntity edg, long now) {
		if (edg.running) {
			edg.burn += litresPerHour((now - edg.runningSince) / 20.0) / TICKS_PER_HOUR * FluidNetworks.DROPLETS_PER_MB;
		}
		if (Math.floorMod(now + edg.getBlockPos().asLong(), 20) != 0) {
			return;
		}
		StorageTankPortBlockEntity tank = dayTank(level, edg);
		boolean changed = false;
		long want = (long) edg.burn;
		if (edg.running && want > 0) {
			long got = 0;
			if (tank != null) {
				try (Transaction t = Transaction.openOuter()) {
					got = tank.portStorage().extract(diesel(), want, t);
					t.commit();
				}
			}
			edg.burn -= got;
			if (got < want) {
				// Out of fuel (or its line shut): the engine dies.
				edg.burn = 0;
				edg.fuelOut = true;
				edg.setDieselRunning(now, false);
				changed = true;
			}
		}
		float share = tank == null || tank.fluid.capacity() <= 0 ? -1F : (float) tank.fluid.amount / tank.fluid.capacity();
		if (Math.abs(share - edg.dayLevel) >= 0.005F || (share < 0) != (edg.dayLevel < 0)) {
			edg.dayLevel = share;
			changed = true;
		}
		if (changed) {
			edg.changed();
		}
	}
}
