package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * An Aura Candle on its brass dish ({@link AuraCandleBlockEntity} keeps its {@link CandleMix} and how long it has
 * burned). Light it with flint and steel or a fire charge; an empty hand snuffs it, keeping what is left. Lit, it gives
 * light by its size ({@link #LIGHT}) and, every {@value #PULSE_TICKS} ticks like a beacon, its aura over {@link #RADIUS}
 * blocks; when it has burned down it goes out in a puff of smoke. Broken, it drops itself as it is, part-burned.
 */
public class AuraCandleBlock extends BaseEntityBlock {
	public static final int MAX_DIPS = 4;
	public static final int PULSE_TICKS = 80;
	public static final int EFFECT_TICKS = 180;
	public static final int HARVEST_DIVISOR = 4;
	/** The aura's radius and the light, by layers. */
	public static final int[] RADIUS = {5, 8, 12, 16};
	public static final int[] LIGHT = {8, 10, 12, 14};
	/** Block event: the aura pulsed (each client draws its ring). */
	public static final int PULSE = 1;
	public static final IntegerProperty DIPS = IntegerProperty.create("dips", 1, MAX_DIPS);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final VoxelShape THIN = Block.box(5.0, 0.0, 5.0, 11.0, 12.0, 11.0);
	private static final VoxelShape WIDE = Block.box(4.0, 0.0, 4.0, 12.0, 13.0, 12.0);

	public AuraCandleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(DIPS, 1).setValue(LIT, false));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT[state.getValue(DIPS) - 1] : 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(DIPS) == MAX_DIPS ? WIDE : THIN;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AuraCandleBlockEntity(pos, state);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		CandleMix mix = context.getItemInHand().get(JugcraftAgriculture.CANDLE_MIX);
		return defaultBlockState().setValue(DIPS, mix == null ? 1 : mix.dips());
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || !state.getValue(LIT) || type != JugcraftAgriculture.AURA_CANDLE_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((AuraCandleBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return CandleLighting.light(stack, state, level, pos, player, hand, LIT);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return CandleLighting.snuff(state, level, pos, player, LIT);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** The aura's ring: a circle of its scents' colour at its edge, drawn by each client when it pulses. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		if (id != PULSE) {
			return super.triggerEvent(state, level, pos, id, param);
		}
		if (level.isClientSide() && level.getBlockEntity(pos) instanceof AuraCandleBlockEntity candle) {
			CandleMix mix = candle.mix();
			int radius = mix.radius();
			int points = Math.min(64, radius * 6);
			for (int i = 0; i < points; i++) {
				float angle = i * Mth.TWO_PI / points;
				int color = mix.scents().isEmpty() ? mix.color() : mix.scents().get(i % mix.scents().size()).color;
				level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | color), pos.getX() + 0.5 + Mth.cos(angle) * radius,
						pos.getY() + 0.3, pos.getZ() + 0.5 + Mth.sin(angle) * radius, 0.0, 0.0, 0.0);
			}
		}
		return true;
	}

	/** Lit, its scents drift up from the flame; a tallow candle smokes (client only). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT) || !(level.getBlockEntity(pos) instanceof AuraCandleBlockEntity candle)) {
			return;
		}
		CandleMix mix = candle.mix();
		double top = pos.getY() + AuraCandleBlockEntity.top(mix) + 0.25;
		if (mix.wax() == CandleWax.TALLOW || random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, top, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
		}
		if (!mix.muddled()) {
			for (CandleScent scent : mix.scents()) {
				if (random.nextInt(3) == 0) {
					level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | scent.color),
							pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, top, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.03, 0.0);
				}
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DIPS, LIT);
	}
}
