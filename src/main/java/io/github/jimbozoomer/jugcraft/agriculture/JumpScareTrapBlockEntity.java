package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Watches the front of a ready {@link JumpScareTrapBlock}: every {@value #PERIOD} ticks it looks at the level's players
 * (no entity search) for one within {@link JumpScareTrapBlock#REACH} blocks of the spot a block in front of it, not
 * sneaking and not a spectator, and springs the trap. It ticks only while the trap is ready, and saves nothing.
 */
public class JumpScareTrapBlockEntity extends BlockEntity {
	public static final int PERIOD = 5;

	public JumpScareTrapBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.JUMP_SCARE_ENTITY, pos, state);
	}

	void serverTick(ServerLevel level) {
		if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), PERIOD) == 0 && someoneInFront(level)) {
			JumpScareTrapBlock.spring(level, worldPosition, getBlockState());
		}
	}

	/** Whether someone (not sneaking, not a spectator) stands within reach of the spot in front of the trap. */
	public boolean someoneInFront(ServerLevel level) {
		Direction facing = getBlockState().getValue(JumpScareTrapBlock.FACING);
		Vec3 front = Vec3.atBottomCenterOf(worldPosition.relative(facing));
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && !player.isSteppingCarefully()
					&& player.position().distanceToSqr(front) <= JumpScareTrapBlock.REACH * JumpScareTrapBlock.REACH) {
				return true;
			}
		}
		return false;
	}
}
