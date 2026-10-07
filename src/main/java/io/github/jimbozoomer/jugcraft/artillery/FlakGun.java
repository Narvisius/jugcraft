package io.github.jimbozoomer.jugcraft.artillery;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Flak Gun (batch 51): twin anti-aircraft cannon on a cross mount. It swings quickly
 * ({@value JugcraftArtillery#FLAK_TRAVERSE} degrees a tick) and fires a Flak Shell every
 * {@value JugcraftArtillery#FLAK_COOLDOWN} ticks while its gunner holds attack; the shells burst beside anything
 * flying.
 */
public class FlakGun extends CrewedGun {
	public FlakGun(EntityType<? extends FlakGun> type, Level level) {
		super(type, level);
	}

	@Override
	protected EntityType<ArtilleryShell> shellType() {
		return JugcraftArtillery.FLAK_SHELL;
	}

	@Override
	protected Item ammo() {
		return JugcraftArtillery.FLAK_SHELL_ITEM;
	}

	@Override
	protected double shellSpeed() {
		return JugcraftArtillery.FLAK_SPEED;
	}

	@Override
	protected double shellGravity() {
		return JugcraftArtillery.FLAK_GRAVITY;
	}

	@Override
	protected int cooldown() {
		return JugcraftArtillery.FLAK_COOLDOWN;
	}

	@Override
	protected float traverse() {
		return JugcraftArtillery.FLAK_TRAVERSE;
	}

	@Override
	protected float minPitch() {
		return -5.0F;
	}

	@Override
	protected float maxPitch() {
		return 85.0F;
	}

	@Override
	protected boolean highArc() {
		return false;
	}

	@Override
	protected boolean automatic() {
		return true;
	}

	@Override
	protected double pivotHeight() {
		return JugcraftArtillery.FLAK_PIVOT_HEIGHT;
	}

	@Override
	protected double barrelLength() {
		return 2.8;
	}

	@Override
	protected Item dropItem() {
		return JugcraftArtillery.FLAK_GUN_ITEM;
	}

	@Override
	protected int health() {
		return JugcraftArtillery.FLAK_HEALTH;
	}

	@Override
	protected int seats() {
		return 1;
	}

	@Override
	protected Vec3 seat(int index) {
		return new Vec3(0, 0.45, -0.9);
	}
}
