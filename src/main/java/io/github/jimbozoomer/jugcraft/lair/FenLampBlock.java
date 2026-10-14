package io.github.jimbozoomer.jugcraft.lair;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fen lamp of the Lantern Fen (docs/features/lantern-fen.md): a great lantern of blackened iron on a willow post, its
 * panes burning with the marsh-light. A lair fixture: it cannot be broken, and any player in the fen lights one that has
 * gone out by using it ({@link #light}). The Mire Hag's fight puts them out, and is told when a player lights one again
 * ({@link #onLitByHand}). Lit, it gives light 15 (tools/lairs.py FEN_LAMP), and now and then a mote of marsh-light drifts
 * up from its cap.
 */
public class FenLampBlock extends LairBrazierBlock {
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);
	private static final List<LitByHand> LIT_BY_HAND = new ArrayList<>();

	/** Told when a player lights a fen lamp that had gone out. */
	@FunctionalInterface
	public interface LitByHand {
		void lit(ServerLevel level, BlockPos pos, Player player);
	}

	/** Calls {@code listener} whenever a player lights a fen lamp that had gone out (the Mire Hag's fight listens). */
	public static void onLitByHand(LitByHand listener) {
		LIT_BY_HAND.add(listener);
	}

	public FenLampBlock(Properties properties) {
		super(properties);
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
		if (level instanceof ServerLevel server && light(level, pos)) {
			for (LitByHand listener : LIT_BY_HAND) {
				listener.lit(server, pos, player);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Lights the lamp at {@code pos} if it has gone out; whether it was lit by this. */
	public static boolean light(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof FenLampBlock) || state.getValue(LIT)) {
			return false;
		}
		level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.8F, 0.7F);
		return true;
	}

	/** Puts the lamp at {@code pos} out if it burns; whether it was put out by this. */
	public static boolean snuff(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof FenLampBlock) || !state.getValue(LIT)) {
			return false;
		}
		level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 1.0F, 0.5F);
		return true;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT) && random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.02, 0.0);
		}
	}
}
