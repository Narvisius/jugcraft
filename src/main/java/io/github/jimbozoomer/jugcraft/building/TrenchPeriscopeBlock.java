package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.agriculture.LongDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import io.github.jimbozoomer.jugcraft.artillery.Spotting;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Trench Periscope (batch 59): two blocks tall, a slim box-section periscope on a stand, its mirror head poking over
 * the parapet and looking the way its placer looked ({@link #FACING}).
 *
 * <p>Every {@value Bunkerworks#PERISCOPE_INTERVAL} ticks the lower half counts the hostile mobs ({@link Enemy}) within
 * {@value Bunkerworks#PERISCOPE_RANGE} blocks, inside a cone of {@value Bunkerworks#PERISCOPE_CONE} degrees either side
 * of its facing, that the mirror head has a clear line of sight to. A comparator reads the count ({@link #SEEN}, at most
 * 15). Using either half looks through it: the nearest mob in view becomes the user's target mark, as a Range Finder
 * marks one ({@link Spotting}), so gunners and fire control tables can use it; sneaking clears the mark.
 */
public class TrenchPeriscopeBlock extends TallDecorationBlock {
	/** How many hostile mobs the periscope sees (lower half only; the upper half keeps 0). */
	public static final IntegerProperty SEEN = IntegerProperty.create("seen", 0, 15);
	private static final double[][] LOWER = {{4, 0, 6, 12, 16, 14}, {6.5, 0, 7, 9.5, 16, 10}};
	private static final double[][] UPPER = {{6.5, 0, 7, 9.5, 10, 10}, {5, 9, 4.5, 11, 16, 10.5}};

	public TrenchPeriscopeBlock(Properties properties) {
		super(properties, Block.box(4, 0, 4, 12, 16, 14), Block.box(5, 0, 4, 11, 16, 11));
		registerDefaultState(defaultBlockState().setValue(SEEN, 0));
	}

	/** It looks the way its placer looks, so its eyepiece faces them. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LongDecorationBlock.shape(state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER, state.getValue(FACING));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!oldState.is(this) && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
			level.scheduleTick(pos, this, Bunkerworks.PERISCOPE_INTERVAL);
		}
	}

	/** The lower half's scan: updates {@link #SEEN} (and the comparator) only when the count changes. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(HALF) != DoubleBlockHalf.LOWER) {
			return;
		}
		int seen = Math.min(15, inView(level, pos, state.getValue(FACING)).size());
		if (seen != state.getValue(SEEN)) {
			level.setBlock(pos, state.setValue(SEEN, seen), Block.UPDATE_ALL);
			level.updateNeighbourForOutputSignal(pos, this);
		}
		level.scheduleTick(pos, this, Bunkerworks.PERISCOPE_INTERVAL);
	}

	/**
	 * Where the mirror head looks from: the middle of the window, just outside the periscope's own block so the line of
	 * sight does not start inside it.
	 */
	public static Vec3 eye(BlockPos lower, Direction facing) {
		return Vec3.atCenterOf(lower.above()).add(0.0, 0.3, 0.0).add(facing.getStepX() * 0.55, 0.0, facing.getStepZ() * 0.55);
	}

	/** The hostile mobs the periscope at {@code lower} sees, nearest first. */
	public static List<LivingEntity> inView(ServerLevel level, BlockPos lower, Direction facing) {
		Vec3 eye = eye(lower, facing);
		Vec3 ahead = new Vec3(facing.getStepX(), 0.0, facing.getStepZ());
		double cos = Math.cos(Math.toRadians(Bunkerworks.PERISCOPE_CONE));
		double range = Bunkerworks.PERISCOPE_RANGE;
		List<LivingEntity> seen = level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(range), mob -> {
			if (!(mob instanceof Enemy) || mob instanceof Player || !mob.isAlive()) {
				return false;
			}
			Vec3 to = mob.getEyePosition().subtract(eye);
			double distance = to.length();
			return distance > 1.0E-3 && distance <= range && to.dot(ahead) >= cos * distance;
		});
		seen.removeIf(mob -> level.clip(new ClipContext(eye, mob.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
				CollisionContext.empty())).getType() != HitResult.Type.MISS);
		seen.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(eye)));
		return seen;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? state.getValue(SEEN) : 0;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** Looks through it: marks the nearest hostile mob in view; sneaking clears the user's mark. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			Spotting.clear(player);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.periscope.cleared"));
			return InteractionResult.SUCCESS;
		}
		BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
		Direction facing = state.getValue(FACING);
		List<LivingEntity> seen = inView(server, lower, facing);
		if (seen.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.periscope.none", Bunkerworks.PERISCOPE_RANGE));
			return InteractionResult.SUCCESS;
		}
		LivingEntity nearest = seen.get(0);
		Vec3 eye = eye(lower, facing);
		Spotting.mark(player, nearest.blockPosition());
		player.sendOverlayMessage(Component.translatable("message.jugcraft.periscope.seen", nearest.getDisplayName(),
				(int) Math.round(nearest.position().distanceTo(eye)),
				Bunkerworks.bearing(nearest.getX() - eye.x, nearest.getZ() - eye.z)));
		level.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SEEN);
	}
}
