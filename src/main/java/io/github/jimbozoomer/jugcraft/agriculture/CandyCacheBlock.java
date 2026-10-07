package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Candy Cache: a hollow, mossy stump with a knot-hole, for hiding treats in a candy hunt. It keeps treats exactly as
 * a {@link CandyBowlBlock} does (the same {@link CandyBowlBlockEntity} rules: fill it with treats, each finder takes one
 * a night, whoever hid it any time), but nothing shows from outside how full it is: only a faint sparkle now and then
 * gives it away.
 */
public class CandyCacheBlock extends CandyBowlBlock {
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0);

	public CandyCacheBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Hidden treats are for finders, not for knocking trick-or-treaters. */
	@Override
	protected boolean invitesTrickOrTreaters() {
		return false;
	}

	@Override
	protected String messages() {
		return "message.jugcraft.candy_cache.";
	}

	/** A faint sparkle at the knot-hole now and then, while there are treats in it. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(FILL) > 0 && random.nextInt(8) == 0) {
			level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.5 + random.nextDouble() * 0.3,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.0, 0.0);
		}
	}
}
