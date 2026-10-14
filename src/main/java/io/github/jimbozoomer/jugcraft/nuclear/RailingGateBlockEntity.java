package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The railing gate's spring: a few times a second it looks for a player at the gate (in its block or just either side
 * of it) and swings the gate open; once nobody has been there for a moment it swings it shut. Opened or shut by hand,
 * it is left alone for two seconds.
 */
public class RailingGateBlockEntity extends BlockEntity {
	/** Ticks the gate is left as it was set by hand. */
	int held;
	private int clear;

	/** Client: the swing (renderer bookkeeping). */
	public boolean animOpen;
	public long animStart = Long.MIN_VALUE;

	public RailingGateBlockEntity(BlockPos pos, BlockState state) {
		super(StorageTanks.GATE_ENTITY, pos, state);
		animOpen = state.getValue(RailingGateBlock.OPEN);
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (held > 0) {
			held--;
			return;
		}
		if (level.getGameTime() % 4 != Math.floorMod(pos.asLong(), 4)) {
			return;
		}
		AABB around = new AABB(pos).inflate(0.9, 0.6, 0.9);
		boolean someone = !level.getEntitiesOfClass(Player.class, around, p -> !p.isSpectator()).isEmpty();
		if (someone) {
			clear = 0;
			RailingGateBlock.swing(level, pos, state, true);
		} else if (state.getValue(RailingGateBlock.OPEN) && ++clear >= 4) {
			RailingGateBlock.swing(level, pos, state, false);
		}
	}
}
