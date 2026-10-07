package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Haunted Dining Chair's watch: once a second, at night, an empty chair with a player within
 * {@value HauntedDiningChairBlock#REACH} blocks slides out; {@value HauntedDiningChairBlock#OUT_TICKS} ticks after
 * sliding out it slides back. On clients, the renderer keeps how far it has slid.
 */
public class HauntedDiningChairBlockEntity extends BlockEntity {
	private long outSince = Long.MIN_VALUE / 2;
	/** On clients: how far out the chair is drawn, 0 to 1, and the game time that was. */
	public float drawnOut;
	public float drawnTime = -1.0F;

	public HauntedDiningChairBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.DINING_CHAIR_ENTITY, pos, state);
	}

	/** Whether a chair may slide out now: at night, empty, a player near, and not out already. */
	public static boolean maySlide(boolean night, boolean occupied, boolean playerNear, boolean out) {
		return night && !occupied && playerNear && !out;
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		long now = level.getGameTime();
		if (state.getValue(HauntedDiningChairBlock.OUT)) {
			if (now - outSince >= HauntedDiningChairBlock.OUT_TICKS) {
				HauntedDiningChairBlock.slide(level, pos, state, false);
			}
			return;
		}
		if (Math.floorMod(now + pos.asLong(), HauntedDiningChairBlock.CHECK_TICKS) != 0) {
			return;
		}
		boolean near = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HauntedDiningChairBlock.REACH, false) != null;
		if (maySlide(MourningAngelBlock.night(level), !Seat.at(level, pos).isEmpty(), near, false)) {
			slideOut(level, pos, state);
		}
	}

	/** Slides the chair out now (the watch above, and tests). */
	public void slideOut(ServerLevel level, BlockPos pos, BlockState state) {
		outSince = level.getGameTime();
		HauntedDiningChairBlock.slide(level, pos, state, true);
	}
}
