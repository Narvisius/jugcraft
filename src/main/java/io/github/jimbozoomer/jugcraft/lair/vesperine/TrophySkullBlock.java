package io.github.jimbozoomer.jugcraft.lair.vesperine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Dirge Skull and the Requiem Skull: Vesperine's trophies, her two great skulls in small, hovering where they are
 * placed with a glow in their sockets (and worn on the head, a costume). Dirge's glow is soul blue, Requiem's red.
 */
public class TrophySkullBlock extends HorizontalDirectionalBlock {
	private static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);
	private final boolean dirge;

	public TrophySkullBlock(boolean dirge, Properties properties) {
		super(properties);
		this.dirge = dirge;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(4) == 0) {
			level.addParticle(dirge ? ParticleTypes.SOUL : ParticleTypes.SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4,
					pos.getY() + 0.2, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, -0.01, 0.0);
		}
	}
}
