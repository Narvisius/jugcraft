package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.agriculture.LongDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Seat;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Bunker Bunk (batch 59): two timber bunks one above the other, each with a khaki blanket on its mattress. Use it to
 * sit on the lower bunk (sneak to get up). It is furniture, not a bed: it sets no spawn point and skips no night.
 */
public class BunkerBunkBlock extends TallDecorationBlock implements Seat.Sittable {
	/** How high above the block's base the sitter sits: on the lower bunk's blanket. */
	public static final double SEAT = 0.45;
	private static final double[][] POSTS = {{0, 0, 0, 2, 16, 2}, {14, 0, 0, 16, 16, 2}, {0, 0, 14, 2, 16, 16}, {14, 0, 14, 16, 16, 16}};
	private static final double[][] LOWER = {POSTS[0], POSTS[1], POSTS[2], POSTS[3], {0, 3, 0, 16, 8, 16}};
	private static final double[][] UPPER = {POSTS[0], POSTS[1], POSTS[2], POSTS[3], {0, 3, 0, 16, 8, 16}, {14, 8, 2, 16, 11, 14}};

	public BunkerBunkBlock(Properties properties) {
		super(properties, Block.box(0, 0, 0, 16, 16, 16), Block.box(0, 0, 0, 16, 16, 16));
	}

	@Override
	public double seatHeight(BlockState state) {
		return SEAT;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LongDecorationBlock.shape(state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER, state.getValue(FACING));
	}

	/** Either half: sit on the lower bunk. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
		if (level instanceof ServerLevel server) {
			BlockState base = level.getBlockState(lower);
			if (!(base.getBlock() instanceof BunkerBunkBlock) || !Seat.sit(server, lower, base, player)) {
				return InteractionResult.PASS;
			}
		}
		return InteractionResult.SUCCESS;
	}
}
