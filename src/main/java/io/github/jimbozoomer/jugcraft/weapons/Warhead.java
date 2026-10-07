package io.github.jimbozoomer.jugcraft.weapons;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What a grenade does when it goes off (batches 18 and 31). The frag grenade's {@link Blast} and the flashbang's
 * {@link Flash} act at once; chlorine, smoke and thermite leave a {@link ChemicalCloud} that works for a few seconds.
 * None of them breaks, moves or burns a block.
 */
public enum Warhead {
	FRAG, CHLORINE, SMOKE, THERMITE, FLASHBANG;

	/** The warhead in a grenade item: a plain grenade (or anything else) is a frag grenade. */
	public static Warhead of(ItemStack stack) {
		return stack.getItem() instanceof GrenadeItem grenade ? grenade.warhead() : FRAG;
	}

	public void detonate(ServerLevel level, Vec3 center, @Nullable Entity direct, @Nullable Entity owner) {
		switch (this) {
			case FRAG -> Blast.detonate(level, center, direct, owner);
			case FLASHBANG -> Flash.detonate(level, center, owner);
			case CHLORINE -> ChemicalCloud.spawn(level, center, ChemicalCloud.Kind.CHLORINE, owner);
			case SMOKE -> ChemicalCloud.spawn(level, center, ChemicalCloud.Kind.SMOKE, owner);
			case THERMITE -> ChemicalCloud.spawn(level, ground(level, center), ChemicalCloud.Kind.THERMITE, owner);
		}
	}

	/** Thermite runs down to the floor: the top of the first solid block within three below {@code center}. */
	private static Vec3 ground(ServerLevel level, Vec3 center) {
		BlockPos pos = BlockPos.containing(center);
		for (int i = 0; i <= 3; i++) {
			BlockPos below = pos.below(i + 1);
			if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
				return new Vec3(center.x, pos.below(i).getY(), center.z);
			}
		}
		return center;
	}
}
