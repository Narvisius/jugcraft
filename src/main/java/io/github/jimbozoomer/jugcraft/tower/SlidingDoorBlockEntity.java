package io.github.jimbozoomer.jugcraft.tower;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A sliding door's sensors ({@link SlidingDoorBlock}): anyone walking up to it from either side opens it, a player or
 * any mob that isn't hostile (the tower's crew walk through; monsters are left outside); it stays open while anyone is
 * in the doorway or near it and slides shut a moment after they have gone. The client times the leaves' slide from when
 * it sees {@link SlidingDoorBlock#OPEN} change (client/SlidingDoorRenderer).
 */
public class SlidingDoorBlockEntity extends BlockEntity {
	/** How far out from the door's line (blocks), either side, someone opens it (the entrance a little farther). */
	public static final double REACH = 1.8, ENTRANCE_REACH = 2.6;
	/** Ticks the doorway must be clear before it shuts. */
	public static final int CLOSE_AFTER = 20;
	/** Client only: what the renderer last saw, and when it changed. */
	public boolean seenOpen;
	public long seenAt = Long.MIN_VALUE;
	/** Client only: how deep a room door's lining runs each side and over it (the wall's own depth there), and when worked out. */
	public final float[] lining = {0, 1, 0, 1, 0, 1};
	public long liningAt = Long.MIN_VALUE;
	private int clearFor;

	public SlidingDoorBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftTower.SLIDING_DOOR_ENTITY, pos, state);
	}

	public SlidingDoorStyle style() {
		return getBlockState().getBlock() instanceof SlidingDoorBlock door ? door.style : SlidingDoorStyle.DOUBLE_GLASS;
	}

	/**
	 * Where a point is relative to the door: how far out past its line (+ outside), how far along it from its middle, how
	 * high above its floor.
	 */
	static double[] relative(BlockPos master, Direction facing, SlidingDoorStyle style, Vec3 at) {
		int[] along = AisleContainmentDoorBlock.turn(1, 0, facing);
		double lineZ = style.entrance() ? SlidingDoorStyle.E_LEAF_Z : 0;
		// the door's middle on its line: half its width along from the master cell's near corner, its line in from the outside face
		Vec3 corner = Vec3.atBottomCenterOf(master).add(-along[0] * 0.5, 0, -along[1] * 0.5)
				.add(facing.getStepX() * (0.5 - lineZ), 0, facing.getStepZ() * (0.5 - lineZ));
		Vec3 middle = corner.add(along[0] * style.width / 2.0, 0, along[1] * style.width / 2.0);
		Vec3 d = at.subtract(middle);
		return new double[] {d.x * facing.getStepX() + d.z * facing.getStepZ(), d.x * along[0] + d.z * along[1], d.y};
	}

	/** Whether {@code e} opens doors: players (not watching), and mobs that aren't hostile. */
	static boolean opens(LivingEntity e) {
		if (e instanceof Player p) {
			return !p.isSpectator();
		}
		return e.isAlive() && !(e instanceof Enemy) && !(e instanceof ArmorStand);
	}

	void serverTick(ServerLevel level, BlockState state) {
		if (level.getGameTime() % 2 != 0) {
			return;
		}
		SlidingDoorStyle style = style();
		Direction facing = state.getValue(SlidingDoorBlock.FACING);
		BlockPos pos = getBlockPos();
		double reach = style.entrance() ? ENTRANCE_REACH : REACH;
		AABB around = new AABB(pos).inflate(style.width + reach, style.height + 1, style.width + reach);
		List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, around, SlidingDoorBlockEntity::opens);
		boolean someone = false;
		double half = style.width / 2.0 + 0.3;
		for (LivingEntity e : near) {
			double[] r = relative(pos, facing, style, e.position());
			if (Math.abs(r[0]) <= reach && Math.abs(r[1]) <= half && r[2] > -0.6 && r[2] < style.height + 0.2) {
				someone = true;
				break;
			}
		}
		boolean open = state.getValue(SlidingDoorBlock.OPEN);
		if (someone) {
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
		level.setBlock(pos, state.setValue(SlidingDoorBlock.OPEN, open), Block.UPDATE_ALL);
		// the parts' collision follows the master's: tell them (and their neighbours) it changed
		for (var o : SlidingDoorBlock.footprint(style(), state.getValue(SlidingDoorBlock.FACING))) {
			BlockPos at = pos.offset(o);
			level.sendBlockUpdated(at, level.getBlockState(at), level.getBlockState(at), Block.UPDATE_ALL);
		}
		level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.3F,
				open ? 1.8F : 1.6F);
	}

	/** Client: how far open the leaves are now (0 shut, 1 slid all the way), eased. */
	public float openness(long gameTime, float partialTick) {
		boolean open = getBlockState().getValue(SlidingDoorBlock.OPEN);
		int ticks = style().slideTicks();
		if (seenAt == Long.MIN_VALUE) {
			seenOpen = open;
			seenAt = gameTime - ticks;
		} else if (open != seenOpen) {
			seenOpen = open;
			seenAt = gameTime;
		}
		float t = Math.min(1, Math.max(0, (gameTime - seenAt + partialTick) / ticks));
		t = t * t * (3 - 2 * t);
		return open ? t : 1 - t;
	}
}
