package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Giant Fake Spider: a big hairy spider, a block and a half across, that dangles on a silk thread from a ceiling,
 * a branch or a cobweb (Spun Cobwebs too) and sways slowly round. Sneak-use the knot where its thread is tied to let
 * it down further ({@link #DROP}, 1 to {@value #MAX_DROP} blocks, then back up). Only the knot is a block; the spider
 * is drawn by each client (client/GiantFakeSpiderRenderer.java), so it can't be bumped into.
 */
public class GiantFakeSpiderBlock extends BaseEntityBlock {
	public static final int MAX_DROP = 4;
	/** Ticks for one slow swing to and fro. */
	public static final int SWAY_PERIOD = 120;
	public static final float SWAY_DEGREES = 6.0F;
	public static final IntegerProperty DROP = IntegerProperty.create("drop", 1, MAX_DROP);
	private static final VoxelShape KNOT = Block.box(5.0, 12.0, 5.0, 11.0, 16.0, 11.0);

	public GiantFakeSpiderBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(DROP, 1));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.GIANT_FAKE_SPIDER_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return KNOT;
	}

	/** It hangs from the underside of a solid block, leaves, or a cobweb. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos above = pos.above();
		BlockState holder = level.getBlockState(above);
		return holder.is(BlockTags.LEAVES) || holder.is(Blocks.COBWEB) || holder.is(JugcraftAgriculture.block("spun_cobweb"))
				|| holder.isFaceSturdy(level, above, Direction.DOWN);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.UP && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** Sneak-use lets the thread out a block further, and from the longest back to the shortest. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(DROP, state.getValue(DROP) % MAX_DROP + 1), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.SPIDER_STEP, SoundSource.BLOCKS, 0.5F, 0.8F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** How far the spider swings from straight down at {@code time} (ticks), in degrees, about x and about z. */
	public static float[] sway(BlockPos pos, float time) {
		double phase = (pos.hashCode() & 0xFF) / 256.0 * Math.PI * 2;
		double t = time / SWAY_PERIOD * Math.PI * 2;
		return new float[] {(float) (SWAY_DEGREES * Math.sin(t + phase)), (float) (SWAY_DEGREES * 0.6 * Math.sin(t * 0.7 + phase * 2))};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DROP);
	}
}
