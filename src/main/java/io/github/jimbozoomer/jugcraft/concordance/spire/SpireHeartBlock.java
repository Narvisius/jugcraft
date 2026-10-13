package io.github.jimbozoomer.jugcraft.concordance.spire;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Concord Spire's heart (roadmap step 25): the block a spire is founded at and raised around. An empty hand founds
 * it (sneaking first to choose the configuration), shows how it stands, or (its keeper sneaking) shares it with their
 * party; the configuration's upkeep item used on it goes into its store, as couriers' deliveries and hoppers do. It is
 * lit while its field works. GeckoLib draws it; the rest is {@link SpireHeartBlockEntity} and {@link ConcordSpire}.
 */
public class SpireHeartBlock extends BaseEntityBlock {
	/** Whether its field works (its light). */
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	/** The whole block, as its model fills it (the foot and the top plate are the full width). */
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

	public SpireHeartBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpireHeartBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, ConcordSpire.HEART_ENTITY, (tickLevel, pos, tickState, heart) -> heart.serverTick((ServerLevel) tickLevel));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel serverLevel && placer instanceof ServerPlayer player
				&& level.getBlockEntity(pos) instanceof SpireHeartBlockEntity heart) {
			heart.placed(player, serverLevel);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND || stack.isEmpty()) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof SpireHeartBlockEntity heart) {
			if (!RateGate.allow(server, "spire", 6)) {
				return InteractionResult.FAIL;
			}
			if (!heart.offer(server, serverLevel, stack)) {
				return InteractionResult.PASS;
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof SpireHeartBlockEntity heart) {
			if (!RateGate.allow(server, "spire", 6)) {
				return InteractionResult.FAIL;
			}
			heart.use(server, serverLevel, player.isShiftKeyDown());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	/**
	 * What the heart shows of its state (roadmap step 27; client only, from the status the server sent, as the
	 * Concordance's presentation settings allow): glyphs drawn in while it is raised; light rising while its field works;
	 * grey ash sinking while its upkeep or attendance has lapsed; sparks and smoke thrown out while it is damaged, which
	 * shows at any intensity. Unfounded or switched off, it shows nothing.
	 */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!(level.getBlockEntity(pos) instanceof SpireHeartBlockEntity heart)) {
			return;
		}
		String status = heart.status();
		boolean damaged = status.equals("damaged");
		boolean lapsed = status.equals("unattended") || status.equals("unsupplied");
		boolean raising = status.equals("raising");
		boolean active = status.equals("active");
		if (!(damaged || lapsed || raising || active) || !Presentation.ambient(random, damaged ? 3 : 4, 12, damaged)) {
			return;
		}
		boolean calm = Presentation.reducedMotion();
		double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
		double y = pos.getY() + 0.6 + random.nextDouble() * 0.6;
		double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
		if (damaged) {
			level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, calm ? 0.0 : 0.03, 0.0);
			level.addParticle(ParticleTypes.CRIT, x, y, z, (random.nextDouble() - 0.5) * 0.3, 0.15, (random.nextDouble() - 0.5) * 0.3);
		} else if (lapsed) {
			level.addParticle(ParticleTypes.WHITE_ASH, x, y + 0.4, z, 0.0, -0.02, 0.0);
		} else if (raising) {
			double cx = pos.getX() + 0.5;
			double cz = pos.getZ() + 0.5;
			// ENCHANT glyphs fly towards the point given as their speed's origin: here, the heart.
			level.addParticle(ParticleTypes.ENCHANT, cx, pos.getY() + 1.0, cz, x - cx, y - pos.getY() - 1.0, z - cz);
		} else {
			level.addParticle(ParticleTypes.END_ROD, x, y + 0.4, z, 0.0, calm ? 0.0 : 0.02, 0.0);
		}
	}
}
