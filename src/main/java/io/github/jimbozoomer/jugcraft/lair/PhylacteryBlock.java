package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Crypt Lich's phylactery (docs/features/crypt-archive.md): an urn of bone banded with gold on the plinth in the
 * middle of the archive's dais, soul-fire burning in its windows. A lair fixture that cannot be broken or used. Whole, it
 * gives light {@value #LIGHT} (tools/lairs.py PHYLACTERY) and a soul now and then drifts up from it; {@link #CRACKED} (the
 * Lich's fight cracks it when his chains are broken), it is dark.
 */
public class PhylacteryBlock extends Block {
	public static final BooleanProperty CRACKED = BooleanProperty.create("cracked");
	public static final int LIGHT = 10;
	private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 12, 12);

	public PhylacteryBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CRACKED, false));
	}

	/** The light the phylactery gives in {@code state}. */
	public static int light(BlockState state) {
		return state.getValue(CRACKED) ? 0 : LIGHT;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Cracks or mends the phylactery at {@code pos}; whether it was the phylactery and it changed. */
	public static boolean setCracked(Level level, BlockPos pos, boolean cracked) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof PhylacteryBlock) || state.getValue(CRACKED) == cracked) {
			return false;
		}
		level.setBlock(pos, state.setValue(CRACKED, cracked), Block.UPDATE_ALL);
		return true;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(CRACKED) && random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.SOUL, pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.8,
					pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0.0, 0.03, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CRACKED);
	}
}
