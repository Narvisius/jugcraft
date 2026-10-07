package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Toilet Paper Streamer: white paper hanging from leaves or a log (block tag {@link #HANGS_FROM}), in strands up to
 * {@value #MAX_LENGTH} blocks long, or draped over a fence or wall below it ({@link #DRAPED}, block tag
 * {@link #DRAPES_OVER}). Thrown Toilet Paper Rolls and pranking trick-or-treaters put them up ({@link #drape}); nothing
 * walks into them, they break at a touch and drop nothing, and rain washes off the ones it reaches.
 */
public class ToiletPaperStreamerBlock extends Block {
	public static final int MAX_LENGTH = 3;
	public static final BooleanProperty DRAPED = BooleanProperty.create("draped");
	public static final TagKey<Block> HANGS_FROM = TagKey.create(Registries.BLOCK, Jugcraft.id("toilet_paper_hangs_from"));
	public static final TagKey<Block> DRAPES_OVER = TagKey.create(Registries.BLOCK, Jugcraft.id("toilet_paper_drapes_over"));
	private static final VoxelShape HANGING = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
	private static final VoxelShape OVER = Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0);

	public ToiletPaperStreamerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(DRAPED, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(DRAPED) ? OVER : HANGING;
	}

	/** Whether a hanging streamer could hang at {@code pos}: under leaves, a log or another hanging streamer. */
	public static boolean canHang(LevelReader level, BlockPos pos) {
		BlockState above = level.getBlockState(pos.above());
		return above.is(HANGS_FROM) || above.getBlock() instanceof ToiletPaperStreamerBlock && !above.getValue(DRAPED);
	}

	public static boolean canDrape(LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).is(DRAPES_OVER);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return state.getValue(DRAPED) ? canDrape(level, pos) : canHang(level, pos);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		if (canDrape(context.getLevel(), context.getClickedPos())) {
			return defaultBlockState().setValue(DRAPED, true);
		}
		return canHang(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	/** Rain that reaches a streamer washes it off, now and then (the strand below it falls with it). */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.isRainingAt(pos) && random.nextInt(3) == 0) {
			level.removeBlock(pos, false);
		}
	}

	/**
	 * Drapes up to {@code count} streamers on the spots within {@code reach} blocks of {@code center} where they can go:
	 * over fences and walls, or hanging from leaves and logs in strands of one to {@value #MAX_LENGTH} blocks. Looks at
	 * a box {@code 2 * reach + 1} blocks across once. Returns how many spots were papered.
	 */
	public static int drape(ServerLevel level, BlockPos center, int reach, int count, RandomSource random) {
		Block streamer = JugcraftAgriculture.block("toilet_paper_streamer");
		List<BlockPos> spots = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
			if (level.getBlockState(pos).isAir() && (canDrape(level, pos) || level.getBlockState(pos.above()).is(HANGS_FROM))) {
				spots.add(pos.immutable());
			}
		}
		int papered = 0;
		while (papered < count && !spots.isEmpty()) {
			BlockPos pos = spots.remove(random.nextInt(spots.size()));
			if (!level.getBlockState(pos).isAir()) {
				continue;
			}
			if (canDrape(level, pos)) {
				level.setBlock(pos, streamer.defaultBlockState().setValue(DRAPED, true), Block.UPDATE_ALL);
			} else {
				int length = 1 + random.nextInt(MAX_LENGTH);
				for (int i = 0; i < length && level.getBlockState(pos.below(i)).isAir(); i++) {
					level.setBlock(pos.below(i), streamer.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
			level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6F, 1.6F);
			papered++;
		}
		return papered;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DRAPED);
	}
}
