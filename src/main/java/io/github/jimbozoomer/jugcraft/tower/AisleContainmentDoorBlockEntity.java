package io.github.jimbozoomer.jugcraft.tower;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An Aisle Containment Door's sensors (AisleContainmentDoorBlock): anyone walking up to it from either side, inside the
 * aisle or out, opens it (a player, or a walker that isn't hostile: the tower's technicians work in the aisle); it stays open while anyone is in the doorway or near it and slides shut a moment after they
 * have gone. The client times the leaves' slide from when it sees {@link AisleContainmentDoorBlock#OPEN} change
 * (client/AisleContainmentDoorRenderer.java).
 */
public class AisleContainmentDoorBlockEntity extends BlockEntity {
	/** How far out from the door's line (blocks), either side, someone opens it; how far across from the aisle's middle. */
	public static final double REACH = 1.7, ACROSS = 1.2;
	/** Ticks the doorway must be clear before it shuts, and the leaves' slide. */
	public static final int CLOSE_AFTER = 16, SLIDE_TICKS = 14;
	private int clearFor;
	/** Client only: what the renderer last saw, and when it changed. */
	public boolean seenOpen;
	public long seenAt = Long.MIN_VALUE;

	public AisleContainmentDoorBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftTower.AISLE_CONTAINMENT_DOOR_ENTITY, pos, state);
	}

	/** Where someone is relative to the door: how far out past its line (+ outside), how far across from the aisle's middle, how high. */
	static double[] relative(BlockPos master, Direction facing, Vec3 at) {
		// The door's line in its model (facing north): the aisle's middle x 16, z 16 less the frame's half depth.
		int[] across = AisleContainmentDoorBlock.turn(1, 0, facing);
		Vec3 line = Vec3.atBottomCenterOf(master).add(across[0] * 0.5, 0, across[1] * 0.5)
				.add(-facing.getStepX() * (0.5 - AisleContainmentDoorBlock.DEPTH / 32), 0, -facing.getStepZ() * (0.5 - AisleContainmentDoorBlock.DEPTH / 32));
		Vec3 d = at.subtract(line);
		return new double[] {d.x * facing.getStepX() + d.z * facing.getStepZ(), d.x * across[0] + d.z * across[1], d.y};
	}

	void serverTick(ServerLevel level, BlockState state) {
		if (level.getGameTime() % 2 != 0) {
			return;
		}
		// Anyone the sliding doors open for opens it (SlidingDoorBlockEntity.opens: players, and mobs that aren't hostile):
		// it used to open for players only, so the tower's technicians could never walk into the cold aisle they work in
		// (10 November 2026).
		Direction facing = state.getValue(AisleContainmentDoorBlock.FACING);
		BlockPos pos = getBlockPos();
		AABB around = new AABB(pos).inflate(3, 2, 3);
		List<net.minecraft.world.entity.LivingEntity> walkers = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, around,
				SlidingDoorBlockEntity::opens);
		boolean near = false;
		for (net.minecraft.world.entity.LivingEntity walker : walkers) {
			double[] r = relative(pos, facing, walker.position());
			if (Math.abs(r[0]) <= REACH && Math.abs(r[1]) <= ACROSS && r[2] > -0.6 && r[2] < 2.2) {
				near = true;
			}
		}
		boolean open = state.getValue(AisleContainmentDoorBlock.OPEN);
		if (near) {
			clearFor = 0;
			if (!open) {
				set(level, state, true);
			}
		} else if (open) {
			clearFor += 2;
			if (clearFor >= CLOSE_AFTER) {
				set(level, state, false);
			}
		}
	}

	private void set(ServerLevel level, BlockState state, boolean open) {
		BlockPos pos = getBlockPos();
		level.setBlock(pos, state.setValue(AisleContainmentDoorBlock.OPEN, open), Block.UPDATE_ALL);
		// the parts' collision follows the master's: tell them (and their neighbours) it changed
		for (var o : AisleContainmentDoorBlock.footprint(state.getValue(AisleContainmentDoorBlock.FACING))) {
			BlockPos at = pos.offset(o);
			level.sendBlockUpdated(at, level.getBlockState(at), level.getBlockState(at), Block.UPDATE_ALL);
		}
		level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.3F,
				open ? 1.8F : 1.6F);
	}

	/** Client: how far open the leaves are now (0 shut, 1 over their racks), eased. */
	public float openness(long gameTime, float partialTick) {
		boolean open = getBlockState().getValue(AisleContainmentDoorBlock.OPEN);
		if (seenAt == Long.MIN_VALUE) {
			seenOpen = open;
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
