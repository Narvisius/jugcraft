package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A plush prize from the midway (fall addition 26): a stuffed felt toy that sits facing whoever places it. Squeezing it
 * (using it with an empty hand) gives a soft squeak and a heart. Won at the High Striker and Ring Toss, not crafted.
 */
public class PlushBlock extends HorizontalDirectionalBlock {
	private final VoxelShape north;
	private final VoxelShape east;
	private final VoxelShape south;
	private final VoxelShape west;

	/** A plush whose footprint, facing north, is {@code plush}'s, turned with it to face any way. */
	public PlushBlock(Properties properties, Midway.Plush plush) {
		super(properties);
		int x0 = plush.x0();
		int z0 = plush.z0();
		int x1 = plush.x1();
		int z1 = plush.z1();
		int h = plush.height();
		north = Block.box(x0, 0, z0, x1, h, z1);
		east = Block.box(16 - z1, 0, x0, 16 - z0, h, x1);
		south = Block.box(16 - x1, 0, 16 - z1, 16 - x0, h, 16 - z0);
		west = Block.box(z0, 0, 16 - x1, z1, h, 16 - x0);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** A squeeze: a squeak and a heart. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server) {
			server.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 0.8F, 1.8F + 0.2F * server.getRandom().nextFloat());
			server.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 1, 0.1, 0.05, 0.1, 0.0);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case EAST -> east;
			case SOUTH -> south;
			case WEST -> west;
			default -> north;
		};
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
		builder.add(FACING);
	}
}
