package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import net.minecraft.world.level.material.Fluid;

/**
 * What each fluid-burning generator burns, in JE per millibucket (KE for the diesel engine) (FLUID_FUELS in tools/petro.py). A generator makes
 * its fixed output every tick and burns as much fuel as that takes, so a fuel with half the JE per mB lasts half as
 * long. Kept in one place so every fuel's value can be audited together (docs/BALANCE.md).
 */
public final class FluidFuels {
	/** Diesel in the diesel generator: 256,000 JE a bucket, a bucket every 1,000 ticks at full output. */
	public static final int DIESEL = 256;
	/** Heavy fuel oil in the diesel generator burns at half diesel's value. */
	public static final int HEAVY_FUEL_OIL = 128;
	/** Gasoline in the gas turbine: the richest fuel, 384,000 JE a bucket. */
	public static final int GASOLINE = 384;
	/** Refinery gas in the gas turbine: half gasoline's value, so the distillation tower's gas is worth burning. */
	public static final int REFINERY_GAS = 192;
	/** Hydrogen in the fuel cell: 128,000 JE a bucket, less than the 204,800 JE the electrolytic cell spends making
	 * a bucket of it (four buckets of brine), so electrolysis and the fuel cell are never a power loop. */
	public static final int HYDROGEN = 128;
	/** The gas turbine's lubricant upkeep: 1 mB for every this many ticks it runs (a bucket lasts 20,000 ticks). */
	public static final int LUBRICANT_TICKS = 20;
	/** The advanced combustion engine (KE per mB): turbocharged, it gets a quarter more from diesel than the diesel
	 * engine, and burns gasoline. Through a magnet dynamo that is 304 and 426 JE per mB: the best use of each fuel, and
	 * the fuel is still used up, so never a loop. */
	public static final int ADVANCED_DIESEL = 320;
	public static final int ADVANCED_GASOLINE = 448;

	private FluidFuels() {
	}

	/** JE per mB of {@code fluid} in this generator, or 0 if it does not burn it. */
	public static int jePerMb(MachineKind kind, Fluid fluid) {
		return switch (kind) {
			case DIESEL_GENERATOR -> fluid == PetroFluids.DIESEL.source() ? DIESEL
					: fluid == PetroFluids.HEAVY_FUEL_OIL.source() ? HEAVY_FUEL_OIL : 0;
			case DIESEL_ENGINE -> fluid == PetroFluids.DIESEL.source() ? DIESEL
					: fluid == PetroFluids.HEAVY_FUEL_OIL.source() ? HEAVY_FUEL_OIL : 0;
			case FUEL_CELL -> fluid == PetroFluids.HYDROGEN.fluid() ? HYDROGEN : 0;
			case ADVANCED_ENGINE -> fluid == PetroFluids.GASOLINE.source() ? ADVANCED_GASOLINE
					: fluid == PetroFluids.DIESEL.source() ? ADVANCED_DIESEL : 0;
			case GAS_TURBINE -> fluid == PetroFluids.GASOLINE.source() ? GASOLINE
					: fluid == PetroFluids.REFINERY_GAS.fluid() ? REFINERY_GAS : 0;
			default -> 0;
		};
	}
}
