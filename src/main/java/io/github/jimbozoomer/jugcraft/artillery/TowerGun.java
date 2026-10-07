package io.github.jimbozoomer.jugcraft.artillery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A tower gun (batch 54, docs/features/tower-guns.md): a heavy emplacement that stands on a 3x3 or 5x5 top, such as a
 * tower's. Each kind's numbers are a {@link JugcraftTowerGuns.Spec}; the crewing, aiming and firing are
 * {@link CrewedGun}'s.
 */
public class TowerGun extends CrewedGun {
	private final JugcraftTowerGuns.Spec spec;

	public TowerGun(EntityType<? extends TowerGun> type, Level level) {
		super(type, level);
		spec = JugcraftTowerGuns.specOf(type);
	}

	public JugcraftTowerGuns.Spec spec() {
		return spec;
	}

	/**
	 * Whether a gun of this footprint (blocks a side) centred on {@code at} has a solid top under every block of it:
	 * the top of a tower, a wall or the ground.
	 */
	public static boolean supported(Level level, BlockPos at, int footprint) {
		int half = footprint / 2;
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				BlockPos below = at.offset(dx, -1, dz);
				if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	protected EntityType<ArtilleryShell> shellType() {
		return spec.shellType();
	}

	@Override
	protected Item ammo() {
		return spec.ammo();
	}

	@Override
	protected double shellSpeed() {
		return spec.speed();
	}

	@Override
	protected double shellGravity() {
		return spec.gravity();
	}

	@Override
	protected int cooldown() {
		return spec.cooldown();
	}

	@Override
	protected float traverse() {
		return spec.traverse();
	}

	@Override
	protected float minPitch() {
		return spec.minPitch();
	}

	@Override
	protected float maxPitch() {
		return spec.maxPitch();
	}

	@Override
	protected boolean highArc() {
		return spec.highArc();
	}

	@Override
	protected boolean automatic() {
		return spec.automatic();
	}

	@Override
	protected double pivotHeight() {
		return spec.pivotHeight();
	}

	@Override
	protected double pivotForward() {
		return spec.pivotForward();
	}

	@Override
	protected double barrelLength() {
		return spec.barrelLength();
	}

	@Override
	protected double[] barrelOffsets() {
		return spec.barrels();
	}

	@Override
	protected Item dropItem() {
		return spec.item();
	}

	@Override
	protected int health() {
		return spec.health();
	}

	@Override
	protected int seats() {
		return spec.seats().length;
	}

	@Override
	protected Vec3 seat(int index) {
		return spec.seats()[Math.min(index, spec.seats().length - 1)];
	}
}
