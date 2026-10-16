package io.github.jimbozoomer.jugcraft.lair;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Warden Bell of the Moonlit Ring (docs/features/moonlit-ring.md): an old silver alarm bell hung under a frame in each
 * of the ring's three bell clearings, its pull rope toward the ring ({@link #FACING}). A lair fixture: any player in the
 * ring may ring it. Rung, it tolls across the ring and swings ({@link #RINGING}) for {@value #SWING_TICKS} ticks
 * (tools/lairs.py WARDEN_BELL), and cannot be rung again until it has stopped; whoever tries is told it is still
 * swinging. The Alpha's fight listens ({@link #onRung}) for the bell that flushes him out (part 2).
 */
public class WardenBellBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty RINGING = BooleanProperty.create("ringing");
	/** How long a rung bell swings, and rests before it can be rung again. */
	public static final int SWING_TICKS = 40;
	// The bell under its yoke: the yoke across the top of the cell, the bell hanging from it.
	private static final VoxelShape BELL = Block.box(4, 4, 4, 12, 14, 12);
	private static final VoxelShape ALONG_X = Shapes.or(BELL, Block.box(2, 14, 7, 14, 16, 9));
	private static final VoxelShape ALONG_Z = Shapes.or(BELL, Block.box(7, 14, 2, 9, 16, 14));
	private static final List<Rung> RUNG = new ArrayList<>();

	/** Told when a player rings a bell. */
	@FunctionalInterface
	public interface Rung {
		void rung(ServerLevel level, BlockPos pos, BlockState state, Player player);
	}

	/** Calls {@code listener} whenever a player rings a bell (the Alpha Werewolf's fight listens). */
	public static void onRung(Rung listener) {
		RUNG.add(listener);
	}

	public WardenBellBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RINGING, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_X : ALONG_Z;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (state.getValue(RINGING)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.bell.resting"));
			return InteractionResult.SUCCESS;
		}
		ring(server, pos);
		for (Rung listener : RUNG) {
			listener.rung(server, pos, state, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Rings the bell at {@code pos}: it tolls and swings; whether there was a bell there at rest. */
	public static boolean ring(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof WardenBellBlock) || state.getValue(RINGING)) {
			return false;
		}
		level.setBlock(pos, state.setValue(RINGING, true), Block.UPDATE_ALL);
		level.scheduleTick(pos, state.getBlock(), SWING_TICKS);
		// Heard across the whole ring and into the trees.
		level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 3.0F, 0.7F);
		level.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.5F, 0.8F);
		level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
		return true;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(RINGING)) {
			level.setBlock(pos, state.setValue(RINGING, false), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, RINGING);
	}
}
