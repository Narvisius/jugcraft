package io.github.jimbozoomer.jugcraft.machine.form;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;

/**
 * A fuel a generator form burns (see {@link MachineForm.Builder#fuel}): the fluid, the energy each millibucket of it
 * gives, and the power the generator makes a tick while burning it. A higher output empties the tank faster; it never
 * makes more energy from the same fuel.
 *
 * @param fluid the fluid's registry id
 * @param jePerMb JE each millibucket gives
 * @param jePerTick JE the generator makes each tick it burns this fuel and has room for them
 */
public record FormFuel(Identifier fluid, int jePerMb, int jePerTick) {
	/** Whether {@code fluid} is this fuel. */
	public boolean is(Fluid fluid) {
		return BuiltInRegistries.FLUID.getKey(fluid).equals(this.fluid);
	}
}
