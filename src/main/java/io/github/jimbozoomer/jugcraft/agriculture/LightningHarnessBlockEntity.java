package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Lightning Harness's watch, on the server: every {@value LightningHarnessBlock#CHECK_TICKS} ticks it looks whether a
 * running Tesla Coil in range has thrown an arc since it last looked, and every other tick whether a lightning bolt has
 * struck within {@value LightningHarnessBlock#STRIKE_REACH} blocks; either fires it, at most once a second. On the
 * client it remembers when it last fired and how far down its arcs went, for the renderer.
 */
public class LightningHarnessBlockEntity extends BlockEntity {
	private long lastFired = Long.MIN_VALUE / 2;
	private long lastCoilArc = Long.MIN_VALUE / 2;
	private int lastBolt = -1;
	private long firedAt = Long.MIN_VALUE / 2;
	private int arcDown;

	public LightningHarnessBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.LIGHTNING_HARNESS_ENTITY, pos, state);
	}

	/** Fires now unless it fired within the last {@value LightningHarnessBlock#COOLDOWN_TICKS} ticks; returns whether it did. */
	public boolean fire(ServerLevel level, BlockPos pos) {
		long now = level.getGameTime();
		if (now - lastFired < LightningHarnessBlock.COOLDOWN_TICKS) {
			return false;
		}
		lastFired = now;
		LightningHarnessBlock.discharge(level, pos);
		return true;
	}

	public long lastFired() {
		return lastFired;
	}

	void serverTick(ServerLevel level, BlockPos pos) {
		long now = level.getGameTime();
		if (Math.floorMod(now + pos.asLong(), 2) == 0) {
			List<LightningBolt> bolts = level.getEntitiesOfClass(LightningBolt.class, new AABB(pos).inflate(LightningHarnessBlock.STRIKE_REACH),
					bolt -> bolt.isAlive() && bolt.getId() != lastBolt);
			if (!bolts.isEmpty()) {
				lastBolt = bolts.get(0).getId();
				fire(level, pos);
				return;
			}
		}
		if (Math.floorMod(now + pos.asLong(), LightningHarnessBlock.CHECK_TICKS) != 0) {
			return;
		}
		for (TeslaCoilBlockEntity coil : TeslaCoilBlockEntity.runningNear(level, pos, LightningHarnessBlock.COIL_RANGE)) {
			long arc = coil.arcTime();
			if (arc > lastCoilArc && now - arc <= LightningHarnessBlock.CHECK_TICKS) {
				lastCoilArc = arc;
				fire(level, pos);
				return;
			}
		}
	}

	/** On the client: it fired at {@code time}, its arcs reaching {@code down} blocks below (0: none). */
	public void fired(long time, int down) {
		firedAt = time;
		arcDown = down;
	}

	public long firedAt() {
		return firedAt;
	}

	public int arcDown() {
		return arcDown;
	}
}
