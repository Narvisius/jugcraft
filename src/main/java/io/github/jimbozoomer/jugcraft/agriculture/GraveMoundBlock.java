package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Grave Mound: a low heap of fresh earth over a grave. Walk past it (without sneaking) and a zombie's hand claws up
 * out of it for a few seconds ({@link ScareProp}); a redstone signal holds the hand up. Its hand faces {@link #FACING}.
 * Low enough to walk over.
 */
public class GraveMoundBlock extends BaseEntityBlock implements ScareProp {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final double REACH = 3.0;
	public static final int UP_TICKS = 60;
	public static final int COOLDOWN_TICKS = 100;
	private static final VoxelShape MOUND = Shapes.or(Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0), Block.box(3.0, 3.0, 2.0, 13.0, 5.0, 14.0));
	private static final VoxelShape WITH_HAND = Shapes.or(MOUND, Block.box(6.0, 5.0, 6.0, 11.0, 16.0, 10.0));

	public GraveMoundBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RAISED, false));
	}

	@Override
	public double reach() {
		return REACH;
	}

	@Override
	public int upTicks() {
		return UP_TICKS;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	/** Earth spills and something groans as the hand claws up. */
	@Override
	public void onRaise(ServerLevel level, BlockPos pos, BlockState state) {
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(Blocks.ROOTED_DIRT.defaultBlockState()));
		level.playSound(null, pos, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 0.7F, 0.6F);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(RAISED) ? WITH_HAND : MOUND;
	}

	/** Only the earth is solid; the hand is not. */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return MOUND;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof ScarePropBlockEntity prop) {
			prop.update(server);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ScarePropBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.SCARE_PROP_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((ScarePropBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, RAISED);
	}

	/** At night a grave may stir: a restless spirit rises over it ({@link Spirits#stir}). */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Spirits.stir(level, pos, random);
	}
}
