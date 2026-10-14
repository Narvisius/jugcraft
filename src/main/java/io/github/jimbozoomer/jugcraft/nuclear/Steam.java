package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Steam off a hot coffee standing on a piece of furniture: a wisp now and then from the mug's spot. */
final class Steam {
	private Steam() {
	}

	/** x, y, z: the mug's middle in model pixels for the piece facing north (turned with the piece). */
	static void puff(Level level, BlockPos pos, Direction facing, double x, double y, double z, RandomSource random) {
		if (random.nextInt(3) != 0) {
			return;
		}
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double nx = 16 - z;
			z = x;
			x = nx;
		}
		level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + x / 16 + (random.nextDouble() - 0.5) * 0.04, pos.getY() + y / 16,
				pos.getZ() + z / 16 + (random.nextDouble() - 0.5) * 0.04, 0, 0.012, 0);
	}
}
