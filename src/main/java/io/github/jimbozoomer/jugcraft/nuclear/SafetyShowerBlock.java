package io.github.jimbozoomer.jugcraft.nuclear;

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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The chemistry lab's combination emergency shower and eyewash (tools/admin_rooms.py shower_part; two blocks high,
 * against the wall): pull its handle or push its flag (right-click it) and it runs for {@link #RUN} game ticks, the
 * shower head pouring over whoever stands under it, both eyewash heads jetting into the bowl, the water running. Only
 * its riser stops you: you stand under it.
 */
public class SafetyShowerBlock extends MultiFurnitureBlock {
	public static final int RUN = 120;
	/** The eyewash heads' tops and the shower head's middle (model pixels from its bottom block, facing north). */
	static final double[][] HEADS = {{6.9, 15.6, 10}, {9.1, 15.6, 10}};
	static final double[] SHOWER = {8, 27.0, 10};
	private static final Map<Level, Map<BlockPos, Long>> RUNNING = new WeakHashMap<>();

	public SafetyShowerBlock(Properties properties) {
		super(properties, 1, 2, true);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction f = state.getValue(FACING);
		if (state.getValue(partProperty()) == 0) {
			return Shapes.or(turned(new double[] {7, 0, 2, 9, 16, 4}, f), turned(new double[] {4.4, 13, 4, 11.6, 15.6, 13.6}, f));
		}
		return Shapes.or(turned(new double[] {7, 0, 2, 9, 14, 4}, f), turned(new double[] {4.4, 11, 2, 11.6, 14, 13.6}, f),
				turned(new double[] {6.4, 2.6, 6, 9.6, 12.6, 7.1}, f));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return turned(new double[] {7, 0, 2, 9, 16, 4}, state.getValue(FACING));
	}

	public static long runningUntil(Level level, BlockPos pos) {
		Long end = RUNNING.getOrDefault(level, Map.of()).get(pos);
		return end == null ? -1 : end;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		start((ServerLevel) level, pos.subtract(offset(state.getValue(FACING), state.getValue(partProperty()))));
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Pulls it: it runs (again) for RUN ticks from now (origin: its bottom block). */
	public void start(ServerLevel level, BlockPos origin) {
		boolean was = runningUntil(level, origin) > level.getGameTime();
		RUNNING.computeIfAbsent(level, k -> new HashMap<>()).put(origin.immutable(), level.getGameTime() + RUN);
		if (!was) {
			level.playSound(null, origin.above(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 0.9F);
			level.scheduleTick(origin, this, 1);
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
			double[] w = EyewashBlock.world(head, facing);
			level.sendParticles(ParticleTypes.SPLASH, pos.getX() + w[0], pos.getY() + w[1], pos.getZ() + w[2], 3, 0.02, 0.06, 0.02, 0.15);
		}
		double[] s = EyewashBlock.world(SHOWER, facing);
		level.sendParticles(ParticleTypes.FALLING_WATER, pos.getX() + s[0], pos.getY() + s[1], pos.getZ() + s[2], 10, 0.14, 0.02, 0.14, 0);
		double[] floor = EyewashBlock.world(new double[] {SHOWER[0], 0.2, SHOWER[2]}, facing);
		level.sendParticles(ParticleTypes.SPLASH, pos.getX() + floor[0], pos.getY() + floor[1], pos.getZ() + floor[2], 6, 0.2, 0.02, 0.2, 0.1);
		if ((end - now) % 20 == 0) {
			level.playSound(null, pos.above(), SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.7F, 1.3F);
		}
		level.scheduleTick(pos, this, 2);
	}
}
