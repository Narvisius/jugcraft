package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/**
 * Scarecrows at work: a {@link ScarecrowBlock} keeps crows ({@link Crow}) off the crops within its guard radius, which
 * grows as it is dressed: {@value #BARE} blocks bare, {@value #HEADED} wearing a head (any pumpkin), {@value #LIT} wearing
 * a lit one (a jack o'lantern, or a hand-carved pumpkin with a torch in it), out to {@value #HEIGHT} blocks above or below.
 * Looking for scarecrows reads only the block entities of the (at most nine) loaded chunks in reach; it never loads one.
 * A {@link HayGolem} guards as a scarecrow wearing its head would, wherever it stands.
 */
public final class Scarecrows {
	public static final int BARE = 4;
	public static final int HEADED = 8;
	public static final int LIT = 12;
	public static final int HEIGHT = 6;

	private Scarecrows() {
	}

	/** How far the scarecrow wearing {@code scarecrow}'s head guards. */
	public static int radius(ScarecrowBlockEntity scarecrow) {
		return scarecrow.head().isEmpty() ? BARE : ScarecrowBlockEntity.lit(scarecrow.head()) ? LIT : HEADED;
	}

	/** Whether a scarecrow (or a {@link CropGuard}: a Hay Golem, a Pumpkling) guards {@code pos}: one whose radius reaches it across the ground, and within its height. */
	public static boolean guarded(ServerLevel level, BlockPos pos) {
		int minX = SectionPos.blockToSectionCoord(pos.getX() - LIT);
		int maxX = SectionPos.blockToSectionCoord(pos.getX() + LIT);
		int minZ = SectionPos.blockToSectionCoord(pos.getZ() - LIT);
		int maxZ = SectionPos.blockToSectionCoord(pos.getZ() + LIT);
		for (int cx = minX; cx <= maxX; cx++) {
			for (int cz = minZ; cz <= maxZ; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof ScarecrowBlockEntity scarecrow) {
						BlockPos at = scarecrow.getBlockPos();
						int radius = radius(scarecrow);
						long dx = at.getX() - pos.getX();
						long dz = at.getZ() - pos.getZ();
						if (dx * dx + dz * dz <= (long) radius * radius && Math.abs(at.getY() - pos.getY()) <= HEIGHT) {
							return true;
						}
					}
				}
			}
		}
		for (LivingEntity guard : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(LIT, HEIGHT, LIT),
				entity -> entity instanceof CropGuard && entity.isAlive())) {
			BlockPos at = guard.blockPosition();
			int radius = Math.min(LIT, ((CropGuard) guard).guardRadius());
			long dx = at.getX() - pos.getX();
			long dz = at.getZ() - pos.getZ();
			if (dx * dx + dz * dz <= (long) radius * radius && Math.abs(at.getY() - pos.getY()) <= HEIGHT) {
				return true;
			}
		}
		return false;
	}
}
