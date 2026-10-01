package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;

/**
 * How petroleum fluids look: their own animated still and flowing textures, untinted. Gases are never placed, but
 * get a still texture too so recipe viewers and other mods can draw them.
 */
public final class PetroFluidsClient {
	private PetroFluidsClient() {
	}

	public static void register() {
		for (PetroFluids.Entry entry : PetroFluids.FLUIDS.values()) {
			FluidRenderingRegistry.register(entry.source(), entry.flowing(), new FluidModel.Unbaked(
					new Material(Jugcraft.id("block/" + entry.id() + "_still")),
					new Material(Jugcraft.id("block/" + entry.id() + "_flow")),
					null,
					null));
		}
		for (PetroFluids.Gas gas : PetroFluids.GASES.values()) {
			Material still = new Material(Jugcraft.id("block/" + gas.id() + "_still"));
			FluidRenderingRegistry.register(gas.fluid(), new FluidModel.Unbaked(still, still, null, null));
		}
	}
}
