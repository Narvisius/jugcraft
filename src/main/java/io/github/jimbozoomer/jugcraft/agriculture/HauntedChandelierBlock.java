package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Haunted Chandelier: a wrought-iron ring of {@value #CANDLES} candles that hangs under a block or a chain (it falls
 * if that goes). Flint and steel or a fire charge lights every candle ({@link #LIT}); an empty hand snuffs them. Lit, it
 * gives 3 light for every two burning candles ({@link #BURNING}). At night a draft nobody can find now and then blows
 * every candle out at once (a random tick, one time in {@value #GUST_CHANCE}); then they relight themselves, one every
 * {@value #RELIGHT_TICKS} ticks, until all burn again. The client draws it swaying gently, with a flame on each
 * burning candle (client/HauntedChandelierRenderer.java).
 */
public class HauntedChandelierBlock extends BaseEntityBlock {
	public static final int CANDLES = 8;
	public static final int GUST_CHANCE = 3;
	public static final int RELIGHT_TICKS = 15;
	/** How far it sways either way, in degrees, and the ticks one sway takes. */
	public static final float SWAY_DEGREES = 3.0F;
	public static final int SWAY_PERIOD = 160;
	/** Whether it has been lit (and is not snuffed by hand): a gust blows the candles out, but they come back. */
	public static final BooleanProperty LIT = BooleanProperty.create("lit");
	/** How many candles burn now. */
	public static final IntegerProperty BURNING = IntegerProperty.create("burning", 0, CANDLES);
	/** The ring and its candles, under the chain. */
	private static final VoxelShape SHAPE = Shapes.or(Block.box(1.0, 1.0, 1.0, 15.0, 9.0, 15.0), Block.box(7.0, 9.0, 7.0, 9.0, 16.0, 9.0));
	/** Where each candle's wick is, in pixels: {x, y, z}: round the ring, every 45 degrees (tools/decor7_data.py). */
	private static final float[][] WICKS = wicks();
	public static final float RING_RADIUS = 5.5F;
	public static final float WICK_HEIGHT = 8.5F;

	public HauntedChandelierBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(BURNING, 0));
	}

	private static float[][] wicks() {
		float[][] out = new float[CANDLES][];
		for (int i = 0; i < CANDLES; i++) {
			double angle = Math.toRadians(i * 45.0);
			out[i] = new float[] {8.0F + RING_RADIUS * (float) Math.cos(angle), WICK_HEIGHT, 8.0F + RING_RADIUS * (float) Math.sin(angle)};
		}
		return out;
	}

	/** The wick of candle {@code index}, in pixels: {x, y, z}. */
	public static float[] wick(int index) {
		return WICKS[index];
	}

	/** Three light for every two burning candles: 12 with all eight. */
	public static int light(BlockState state) {
		return state.getValue(BURNING) * 3 / 2;
	}

	/** How far the chandelier at {@code pos} leans at {@code time} (ticks with the partial tick), in degrees: {x, z}. */
	public static float[] sway(BlockPos pos, float time) {
		float phase = (pos.hashCode() & 0xFF) / 256.0F;
		float angle = (time / SWAY_PERIOD + phase) * Mth.TWO_PI;
		return new float[] {SWAY_DEGREES * Mth.sin(angle), SWAY_DEGREES * 0.6F * Mth.cos(angle * 0.7F)};
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.HAUNTED_CHANDELIER_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** It hangs from the underside of a block, as a lantern does (a chain will do). */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.UP && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** Flint and steel or a fire charge lights every candle. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		boolean flint = stack.is(Items.FLINT_AND_STEEL);
		if (!flint && !stack.is(Items.FIRE_CHARGE) || state.getValue(LIT) && state.getValue(BURNING) == CANDLES) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(LIT, true).setValue(BURNING, CANDLES), Block.UPDATE_ALL);
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

	/** An empty hand snuffs them all, and they stay out. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(LIT)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(LIT, false).setValue(BURNING, 0), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(LIT);
	}

	/** At night, one time in {@value #GUST_CHANCE}, a gust blows every candle out. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(BURNING) == CANDLES && MourningAngelBlock.night(level) && random.nextInt(GUST_CHANCE) == 0) {
			gust(level, pos, state);
		}
	}

	/** Blows every candle out; they then relight themselves one by one. */
	public static void gust(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state.setValue(BURNING, 0), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.5F);
		for (float[] wick : WICKS) {
			level.sendParticles(ParticleTypes.SMOKE, pos.getX() + wick[0] / 16, pos.getY() + wick[1] / 16 + 0.05, pos.getZ() + wick[2] / 16,
					2, 0.02, 0.02, 0.02, 0.0);
		}
		level.scheduleTick(pos, state.getBlock(), RELIGHT_TICKS);
	}

	/** One more candle catches by itself, while the chandelier is still meant to be lit. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int burning = state.getValue(BURNING);
		if (!state.getValue(LIT) || burning >= CANDLES) {
			return;
		}
		level.setBlock(pos, state.setValue(BURNING, burning + 1), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.15F, 1.6F + random.nextFloat() * 0.3F);
		if (burning + 1 < CANDLES) {
			level.scheduleTick(pos, this, RELIGHT_TICKS);
		}
	}

	/** Now and then a thread of smoke from a burning candle. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		int burning = state.getValue(BURNING);
		if (burning == 0 || random.nextInt(4) != 0) {
			return;
		}
		float[] wick = WICKS[random.nextInt(burning)];
		level.addParticle(ParticleTypes.SMOKE, pos.getX() + wick[0] / 16, pos.getY() + wick[1] / 16 + 0.15, pos.getZ() + wick[2] / 16, 0.0, 0.0, 0.0);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT, BURNING);
	}
}
