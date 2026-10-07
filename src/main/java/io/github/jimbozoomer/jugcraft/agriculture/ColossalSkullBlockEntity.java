package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Colossal Skull's first block: when its jaw last began to move and when it last snapped, for the client to draw
 * the jaw from (neither is saved: a skull loaded later shows its jaw at rest, open or shut).
 */
public class ColossalSkullBlockEntity extends BlockEntity {
	private long jawMoved = Long.MIN_VALUE / 2;
	private long snapped = Long.MIN_VALUE / 2;

	public ColossalSkullBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.COLOSSAL_SKULL_ENTITY, pos, state);
	}

	public void jaw(long time) {
		jawMoved = time;
	}

	public void snap(long time) {
		snapped = time;
	}

	/** How far open the jaw is at {@code time} (0 shut, 1 open), easing toward where the signal wants it. */
	public float openness(float time, boolean powered) {
		float t = Mth.clamp((time - jawMoved) / ColossalSkullBlock.JAW_TICKS, 0.0F, 1.0F);
		float eased = t * t * (3 - 2 * t);
		float open = powered ? eased : 1.0F - eased;
		float s = (time - snapped) / ColossalSkullBlock.SNAP_TICKS;
		if (s >= 0 && s < 1) {
			open = Math.max(open, Mth.sin(s * Mth.PI) * 0.6F);
		}
		return open;
	}
}
