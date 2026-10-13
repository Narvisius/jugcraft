package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Alembic Crucible (roadmap step 13). Use water, ingredients, a stirring rod, a bottle or bowl, a spoon, an Assay
 * Glass or a formula on it; empty-handed it says how it stands, and sneaking empty-handed stops it repeating a formula.
 * GeckoLib draws it (client/CrucibleRenderer: the liquid at its level, in the colour of its strongest property);
 * everything else is {@link CrucibleBlockEntity}.
 */
public class CrucibleBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 12.0, 15.0);

	public CrucibleBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CrucibleBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftConcordance.CRUCIBLE_ENTITY, (tickLevel, pos, tickState, crucible) -> crucible.serverTick((ServerLevel) tickLevel));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.isEmpty()) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
			if (!RateGate.allow(server, "crucible", 4)) {
				return InteractionResult.FAIL;
			}
			return crucible.use(server, serverLevel, stack, hand);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
			crucible.useEmpty(server, player.isShiftKeyDown());
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
	 * Steam over a hot mixture; over a searing one, which a stir would damage, smoke and spitting sparks: its danger
	 * (roadmap step 27). Client only, as the Concordance's presentation settings allow; the danger shows at any intensity.
	 */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) || crucible.parts() == 0
				|| crucible.band().ordinal() < Band.HOT.ordinal()) {
			return;
		}
		boolean searing = crucible.band() == Band.SEARING;
		if (!Presentation.ambient(random, searing ? 2 : 3, 12, searing)) {
			return;
		}
		double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
		double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
		if (searing) {
			level.addParticle(ParticleTypes.LARGE_SMOKE, x, pos.getY() + 0.8, z, 0.0, Presentation.reducedMotion() ? 0.01 : 0.04, 0.0);
			level.addParticle(ParticleTypes.LAVA, x, pos.getY() + 0.8, z, 0.0, 0.0, 0.0);
		} else {
			level.addParticle(ParticleTypes.CLOUD, x, pos.getY() + 0.8, z, 0.0, Presentation.reducedMotion() ? 0.01 : 0.03, 0.0);
		}
	}
}
