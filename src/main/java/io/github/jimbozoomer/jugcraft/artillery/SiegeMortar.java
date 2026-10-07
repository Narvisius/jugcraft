package io.github.jimbozoomer.jugcraft.artillery;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Siege Mortar (batch 51): a fixed heavy mortar on a concrete ring. Its turntable turns
 * {@value JugcraftArtillery#MORTAR_TRAVERSE} degrees a tick and it lobs Heavy Shells on the high arc, at its gunner's
 * marked target or the block they look at, every {@value JugcraftArtillery#MORTAR_COOLDOWN} ticks.
 */
public class SiegeMortar extends CrewedGun {
	public SiegeMortar(EntityType<? extends SiegeMortar> type, Level level) {
		super(type, level);
	}

	@Override
	protected EntityType<ArtilleryShell> shellType() {
		return JugcraftArtillery.HEAVY_SHELL;
	}

	@Override
	protected Item ammo() {
		return JugcraftArtillery.HEAVY_SHELL_ITEM;
	}

	@Override
	protected double shellSpeed() {
		return JugcraftArtillery.HEAVY_SPEED;
	}

	@Override
	protected double shellGravity() {
		return JugcraftArtillery.HEAVY_GRAVITY;
	}

	@Override
	protected int cooldown() {
		return JugcraftArtillery.MORTAR_COOLDOWN;
	}

	@Override
	protected float traverse() {
		return JugcraftArtillery.MORTAR_TRAVERSE;
	}

	@Override
	protected float minPitch() {
		return 45.0F;
	}

	@Override
	protected float maxPitch() {
		return 85.0F;
	}

	@Override
	protected boolean highArc() {
		return true;
	}

	@Override
	protected double pivotHeight() {
		return JugcraftArtillery.MORTAR_PIVOT_HEIGHT;
	}

	@Override
	protected double barrelLength() {
		return 3.6;
	}

	@Override
	protected Item dropItem() {
		return JugcraftArtillery.SIEGE_MORTAR_ITEM;
	}

	@Override
	protected int health() {
		return JugcraftArtillery.MORTAR_HEALTH;
	}

	@Override
	protected int seats() {
		return 2;
	}

	/** The gunner stands on the deck behind the breech; a loader beside them. */
	@Override
	protected Vec3 seat(int index) {
		return index == 0 ? new Vec3(0, 0.8, -0.95) : new Vec3(0.75, 0.8, -0.6);
	}
}
