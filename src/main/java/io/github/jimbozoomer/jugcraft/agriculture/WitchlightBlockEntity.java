package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A witchlight's watch, on the block that holds the lantern: every {@value Witchlights#CHECK_TICKS} ticks it looks for a
 * player within {@value Witchlights#RANGE} blocks and a redstone signal, and wakes or sleeps the lamp
 * ({@link Witchlights#awake}). On clients the renderer keeps the glow it last drew, to fade between them.
 */
public class WitchlightBlockEntity extends BlockEntity {
	private long lastSeen = Long.MIN_VALUE / 2;
	/** On clients: the glow last drawn, 0 asleep to 1 awake, and when. */
	public float glow = -1.0F;
	public float glowTime;

	public WitchlightBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.WITCHLIGHT_ENTITY, pos, state);
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		long now = level.getGameTime();
		if (Math.floorMod(now + pos.asLong(), Witchlights.CHECK_TICKS) != 0) {
			return;
		}
		look(level, pos, state, now);
	}

	/** Looks now (the tick above, and tests). */
	public void look(ServerLevel level, BlockPos pos, BlockState state, long now) {
		boolean near = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Witchlights.RANGE, false) != null;
		boolean powered = level.hasNeighborSignal(pos)
				|| state.getBlock() instanceof WitchlightLampPostBlock && level.hasNeighborSignal(pos.below());
		if (near || powered) {
			lastSeen = now;
		}
		boolean awake = Witchlights.awake(near, powered, now, lastSeen);
		if (awake != state.getValue(Witchlights.LIT)) {
			Witchlights.wake(level, pos, state, awake);
		}
	}
}
