package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Floating Candles: one to {@value #MAX} candles hanging in the air where they are placed, bobbing gently (the client
 * draws them: client/FloatingCandleRenderer.java). Use another Floating Candle on them to add one, like vanilla
 * candles. Light them with flint and steel or a fire charge ({@value #LIGHT_PER_CANDLE} light a candle while lit);
 * use them with an empty hand to snuff them. They need nothing to stand on and nothing walks into them.
 */
public class FloatingCandleBlock extends BaseEntityBlock {
	public static final int MAX = 4;
	public static final int LIGHT_PER_CANDLE = 3;
	public static final IntegerProperty CANDLES = IntegerProperty.create("candles", 1, MAX);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	/** Each candle's centre (x, z) and the bottom of its wax (y), in pixels, by how many candles there are. */
	private static final float[][][] CANDLE = {
			{{8, 8, 6}},
			{{6, 8, 7}, {10, 8, 5}},
			{{8, 6, 7.5F}, {5.5F, 10, 5}, {10.5F, 10, 6}},
			{{6, 6, 6}, {10, 6, 7.5F}, {6, 10, 8}, {10, 10, 5}}};
	/** How tall each candle's wax is, in pixels, by its place. */
	public static final float[] HEIGHT = {5, 4, 6, 4.5F};
	/** How far a candle rises and falls (pixels), and how many ticks one bob takes. */
	public static final float BOB = 1.0F;
	public static final int BOB_TICKS = 80;
	private static final VoxelShape SHAPE = Block.box(4.0, 4.0, 4.0, 12.0, 15.0, 12.0);

	public FloatingCandleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CANDLES, 1).setValue(LIT, false));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT_PER_CANDLE * state.getValue(CANDLES) : 0;
	}

	/** Where candle {@code index} of {@code count} stands: its centre x and z and the bottom of its wax, in pixels. */
	public static float[] candle(int count, int index) {
		return CANDLE[count - 1][index];
	}

	/**
	 * How far (in blocks) candle {@code index} of the candles at {@code pos} has risen or fallen at game time
	 * {@code gameTime} plus {@code partialTick}: each candle bobs out of step with its neighbours.
	 */
	public static float bob(BlockPos pos, int index, long gameTime, float partialTick) {
		float phase = index * 1.7F + (pos.hashCode() & 0xFF) * 0.37F;
		float time = Math.floorMod(gameTime, BOB_TICKS) + partialTick;
		return Mth.sin(time * Mth.TWO_PI / BOB_TICKS + phase) * BOB / 16.0F;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState there = context.getLevel().getBlockState(context.getClickedPos());
		return there.is(this) ? there.setValue(CANDLES, Math.min(MAX, there.getValue(CANDLES) + 1)) : defaultBlockState();
	}

	/** Another Floating Candle goes in with these, up to {@value #MAX}. */
	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		return !context.isSecondaryUseActive() && context.getItemInHand().is(asItem()) && state.getValue(CANDLES) < MAX
				|| super.canBeReplaced(state, context);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		boolean flint = stack.is(Items.FLINT_AND_STEEL);
		if (!flint && !stack.is(Items.FIRE_CHARGE) || state.getValue(LIT)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
			level.playSound(null, pos, flint ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F,
					level.getRandom().nextFloat() * 0.4F + 0.8F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			if (flint) {
				stack.hurtAndBreak(1, player, hand);
			} else {
				stack.consume(1, player);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** An empty hand snuffs them all. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(LIT)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** Each lit candle's flame flickers at its wick, wherever its bob has taken it. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		int count = state.getValue(CANDLES);
		for (int i = 0; i < count; i++) {
			if (random.nextInt(3) != 0) {
				continue;
			}
			float[] at = candle(count, i);
			double x = pos.getX() + at[0] / 16.0;
			double y = pos.getY() + (at[2] + HEIGHT[i] + 1.0) / 16.0 + bob(pos, i, level.getGameTime(), 0.0F);
			double z = pos.getZ() + at[1] / 16.0;
			level.addParticle(random.nextFloat() < 0.2F ? ParticleTypes.SMOKE : ParticleTypes.SMALL_FLAME, x, y, z, 0.0, 0.0, 0.0);
		}
		if (random.nextFloat() < 0.03F) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS,
					1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FloatingCandleBlockEntity(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CANDLES, LIT);
	}
}
