package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * An iron pot for multi-ingredient meals. It cooks while a heat source (block tag
 * {@code jugcraft:heat_sources}, such as a lit campfire) is directly under it; behaviour lives in
 * {@link CookingPotBlockEntity}. {@link #COOKING} shows broth in the pot while it has a recipe and heat.
 */
public class CookingPotBlock extends BaseEntityBlock {
	public static final BooleanProperty COOKING = BooleanProperty.create("cooking");
	/** The owner's pot (tools/menu_data.py): its body and the lugs each side; the bail handle above is left out. */
	private static final VoxelShape SHAPE = Shapes.or(Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0),
			Block.box(0.5, 6.0, 6.5, 15.5, 8.0, 9.5));

	public CookingPotBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(COOKING, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CookingPotBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftAgriculture.COOKING_POT_ENTITY,
				(tickLevel, pos, tickState, pot) -> pot.serverTick((ServerLevel) tickLevel, pos, tickState));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
			player.openMenu(provider);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** Comparators read how full the result slot is, like a furnace's output. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
		return level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot ? pot.comparatorSignal() : 0;
	}

	/** Steam and bubbles while cooking (client only; nothing is sent over the network). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(COOKING)) {
			return;
		}
		double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
		double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
		level.addParticle(ParticleTypes.WHITE_SMOKE, x, pos.getY() + 0.6, z, 0.0, 0.03, 0.0);
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.5, z, 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COOKING);
	}
}
