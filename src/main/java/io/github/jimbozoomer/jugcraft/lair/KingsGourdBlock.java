package io.github.jimbozoomer.jugcraft.lair;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A King's Gourd of the Crowfield (docs/features/crowfield.md): a great gourd on each of the King's eight hills, facing
 * his post ({@link #FACING}). A lair fixture. Its {@link #STAGE} (tools/lairs.py KINGS_GOURD): {@value #GREEN} green, at
 * rest; {@value #SWELLING} and {@value #RIPENING} ripening, gold-green then orange, glowing; {@value #LIT} lit, its face
 * burning; {@value #PICKED} picked, only the vine left. Any player may pick a ripening gourd by using it; a green, lit or
 * picked one says why not, and nothing changes. The Pumpkin King's fight ripens them and listens ({@link #onPicked}) for
 * the gourds picked (part 2).
 */
public class KingsGourdBlock extends HorizontalDirectionalBlock {
	public static final int GREEN = 0;
	public static final int SWELLING = 1;
	public static final int RIPENING = 2;
	public static final int LIT = 3;
	public static final int PICKED = 4;
	public static final IntegerProperty STAGE = IntegerProperty.create("stage", GREEN, PICKED);
	/** The light of each stage. */
	private static final int[] LIGHT = {0, 4, 8, 13, 0};
	private static final VoxelShape GOURD = Shapes.or(Block.box(2, 0, 2, 14, 10, 14), Block.box(7, 10, 7, 9, 13, 9));
	private static final VoxelShape VINE = Block.box(5, 0, 5, 11, 4, 11);
	private static final List<Picked> PICKED_BY = new ArrayList<>();

	/** Told when a player picks a ripening gourd. */
	@FunctionalInterface
	public interface Picked {
		void picked(ServerLevel level, BlockPos pos, BlockState state, Player player);
	}

	/** Calls {@code listener} whenever a player picks a gourd (the Pumpkin King's fight listens). */
	public static void onPicked(Picked listener) {
		PICKED_BY.add(listener);
	}

	public KingsGourdBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STAGE, GREEN));
	}

	public static int light(BlockState state) {
		return LIGHT[state.getValue(STAGE)];
	}

	/** Whether a gourd at {@code stage} is ripening, and so may be picked. */
	public static boolean ripening(int stage) {
		return stage == SWELLING || stage == RIPENING;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(STAGE) == PICKED ? VINE : GOURD;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		int stage = state.getValue(STAGE);
		if (!ripening(stage)) {
			player.sendOverlayMessage(Component.translatable(stage == GREEN ? "message.jugcraft.lair.gourd.green"
					: stage == LIT ? "message.jugcraft.lair.gourd.lit" : "message.jugcraft.lair.gourd.picked"));
			return InteractionResult.SUCCESS;
		}
		pick(server, pos);
		for (Picked listener : PICKED_BY) {
			listener.picked(server, pos, state, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Picks the gourd at {@code pos}: it comes off its vine; whether there was a ripening gourd there to pick. */
	public static boolean pick(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof KingsGourdBlock) || !ripening(state.getValue(STAGE))) {
			return false;
		}
		level.setBlock(pos, state.setValue(STAGE, PICKED), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 0.6F);
		level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PUMPKIN.defaultBlockState()), pos.getX() + 0.5,
				pos.getY() + 0.4, pos.getZ() + 0.5, 16, 0.3, 0.2, 0.3, 0.05);
		return true;
	}

	/** Sets the gourd at {@code pos} to {@code stage}; whether there was a gourd there (the King's fight, part 2). */
	public static boolean setStage(ServerLevel level, BlockPos pos, int stage) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof KingsGourdBlock)) {
			return false;
		}
		if (state.getValue(STAGE) != stage) {
			level.setBlock(pos, state.setValue(STAGE, stage), Block.UPDATE_ALL);
		}
		return true;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, STAGE);
	}
}
