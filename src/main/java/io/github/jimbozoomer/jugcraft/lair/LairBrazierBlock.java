package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A soul brazier round a lair's arena: a squat black-stone pedestal and an iron bowl of soul fire. A lair fixture (the
 * fight lights and puts them out); it cannot be broken. A player lights one that has gone out by using it (Vesperine's
 * Death's Harvest puts out the four that ward her circle).
 */
public class LairBrazierBlock extends Block {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

	public LairBrazierBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(LIT)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT) && random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.85,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.03, 0.0);
		}
	}
}
