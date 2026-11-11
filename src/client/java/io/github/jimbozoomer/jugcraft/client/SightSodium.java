package io.github.jimbozoomer.jugcraft.client;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;

/**
 * {@link Sight}'s occlusion test when Sodium draws the world (it replaces the game's chunk sections and occlusion graph
 * with its own): Sodium's own test of whether a box touches a section it found visible this frame, the one it culls
 * entities with. Only loaded when Sodium is (Jugcraft runs without it).
 */
final class SightSodium {
	private SightSodium() {
	}

	/** Whether Sodium has a visible section in the box; true when Sodium isn't drawing a world. */
	static boolean visible(double x0, double y0, double z0, double x1, double y1, double z1) {
		SodiumWorldRenderer renderer = SodiumWorldRenderer.instanceNullable();
		return renderer == null || renderer.isBoxVisible(x0, y0, z0, x1, y1, z1);
	}
}
