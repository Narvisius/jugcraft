package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A plumbed emergency eyewash (round 26, #9; Justin: "Eyewash and spill kit"): push its flag (right-click it) and its
 * stay-open valve runs both spray heads for {@link #RUN} game ticks, the water jetting up out of them and splashing in
 * the bowl, the water running. The model is tools/plant_panels.py eyewash_boxes (the heads at EW_HEADS, the bowl's
 * centre EW_C, the piece facing north with the wall behind it at z 0).
 */
public class EyewashBlock extends FurnitureBlock {
	public static final int RUN = 100;
	/** The spray heads' tops (model pixels, the piece facing north). */
	static final double[][] HEADS = {{6.4, 13.4, 7.4}, {9.6, 13.4, 7.4}};
	/** Each level's running eyewashes: until when (game time). */
	private static final Map<Level, Map<BlockPos, Long>> RUNNING = new WeakHashMap<>();

	public EyewashBlock(Properties properties, double[]... boxes) {
		super(properties, boxes);
	}

	/** Until when the eyewash at pos runs (game time), or -1. */
	public static long runningUntil(Level level, BlockPos pos) {
		Long end = RUNNING.getOrDefault(level, Map.of()).get(pos);
		return end == null ? -1 : end;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		start((ServerLevel) level, pos);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Pushes the flag: it runs (again) for RUN ticks from now. */
	public void start(ServerLevel level, BlockPos pos) {
		boolean was = runningUntil(level, pos) > level.getGameTime();
		RUNNING.computeIfAbsent(level, k -> new HashMap<>()).put(pos.immutable(), level.getGameTime() + RUN);
		if (!was) {
			level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.2F);
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		long end = runningUntil(level, pos), now = level.getGameTime();
		if (end < 0 || now >= end || !state.is(this)) {
			RUNNING.getOrDefault(level, new HashMap<>()).remove(pos);
			return;
		}
		Direction facing = state.getValue(FACING);
		for (double[] head : HEADS) {
			double[] w = world(head, facing);
			// The jet up out of the head, falling back into the bowl.
			level.sendParticles(ParticleTypes.SPLASH, pos.getX() + w[0], pos.getY() + w[1], pos.getZ() + w[2], 4, 0.02, 0.06, 0.02, 0.15);
			level.sendParticles(ParticleTypes.FALLING_WATER, pos.getX() + w[0], pos.getY() + w[1] + 0.12, pos.getZ() + w[2], 1, 0.03, 0.02, 0.03, 0);
		}
		if ((end - now) % 20 == 0) {
			level.playSound(null, pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.6F);
		}
		level.scheduleTick(pos, this, 2);
	}

	/** A point of the model (pixels, facing north) in the block, turned to facing (as the blockstate turns the model). */
	static double[] world(double[] p, Direction facing) {
		double x = p[0], z = p[2];
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double nx = 16 - z, nz = x;
			x = nx;
			z = nz;
		}
		return new double[] {x / 16, p[1] / 16, z / 16};
	}
}
