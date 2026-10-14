package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A speed gate's sensors (SpeedGateBlock): anyone walking up to the doors the way through opens them; they stay open
 * while anyone is in the lane and close a moment after it is clear. Coming the other way, they stay shut. The client
 * times the doors' slide from when it sees {@link SpeedGateBlock#OPEN} change (client/SpeedGateRenderer.java).
 */
public class SpeedGateBlockEntity extends BlockEntity {
	/** How far before the doors (blocks) someone coming the way through opens them. */
	public static final double REACH = 1.6;
	/** Ticks the lane must be clear before the doors close. */
	public static final int CLOSE_AFTER = 10;
	/** Ticks the doors take to slide open or shut. */
	public static final int SLIDE_TICKS = 8;
	private int clearFor;
	/** Client only: what the renderer last saw, and when it changed. */
	public boolean seenOpen;
	public long seenAt = Long.MIN_VALUE;

	public SpeedGateBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.SPEED_GATE_ENTITY, pos, state);
	}

	/** Where someone is relative to the doors: how far along the way through past them, and how far off the middle. */
	static double[] relative(BlockPos lane, Direction facing, Vec3 at) {
		Vec3 line = Vec3.atBottomCenterOf(lane).add(facing.getStepX() * 0.5, 0, facing.getStepZ() * 0.5);
		Vec3 d = at.subtract(line);
		Direction right = facing.getClockWise();
		return new double[] {d.x * facing.getStepX() + d.z * facing.getStepZ(), d.x * right.getStepX() + d.z * right.getStepZ(), d.y};
	}

	void serverTick(ServerLevel level, BlockState state) {
		if (level.getGameTime() % 2 != 0) {
			return;
		}
		Direction facing = state.getValue(SpeedGateBlock.FACING);
		BlockPos pos = getBlockPos();
		AABB around = new AABB(pos).inflate(2.5, 1, 2.5);
		List<Player> players = level.getEntitiesOfClass(Player.class, around, p -> !p.isSpectator());
		boolean coming = false, inLane = false;
		for (Player player : players) {
			double[] r = relative(pos, facing, player.position());
			if (Math.abs(r[1]) > 0.8 || r[2] < -0.5 || r[2] > 1.5) {
				continue;
			}
			if (r[0] >= -REACH && r[0] <= 0.3) {
				coming = true;
			}
			if (r[0] >= -REACH && r[0] <= 1.6) {
				inLane = true;
			}
		}
		boolean open = state.getValue(SpeedGateBlock.OPEN);
		if (!open && coming) {
			clearFor = 0;
			set(level, state, true);
		} else if (open) {
			clearFor = inLane ? 0 : clearFor + 2;
			if (clearFor >= CLOSE_AFTER) {
				set(level, state, false);
			}
		}
	}

	private void set(ServerLevel level, BlockState state, boolean open) {
		BlockPos pos = getBlockPos();
		level.setBlock(pos, state.setValue(SpeedGateBlock.OPEN, open), Block.UPDATE_ALL);
		level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.35F,
				open ? 1.7F : 1.5F);
	}

	/** Client: how far open the doors are now (0 shut, 1 in their pedestals), eased. */
	public float openness(long gameTime, float partialTick) {
		boolean open = getBlockState().getValue(SpeedGateBlock.OPEN);
		if (seenAt == Long.MIN_VALUE) {
			seenOpen = open;               // first seen: as it is, not sliding
			seenAt = gameTime - SLIDE_TICKS;
		} else if (open != seenOpen) {
			seenOpen = open;
			seenAt = gameTime;
		}
		float t = Math.min(1, Math.max(0, (gameTime - seenAt + partialTick) / SLIDE_TICKS));
		t = t * t * (3 - 2 * t);
		return open ? t : 1 - t;
	}
}
