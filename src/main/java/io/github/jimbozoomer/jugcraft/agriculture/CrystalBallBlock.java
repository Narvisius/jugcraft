package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Crystal Ball: a glass orb full of violet mist on a gilt claw stand, glowing softly (light {@value #LIGHT}). Use
 * it to gaze into it: the mist flares ({@link #GAZING}, light {@value #GAZING_LIGHT}) and it tells the gazer one of
 * {@value #FORTUNES} fortunes, chosen on the server. Then it rests {@value #GAZE_TICKS} ticks before it can be read
 * again, so nobody can flood the area with its chimes.
 */
public class CrystalBallBlock extends Block {
	public static final int LIGHT = 6;
	public static final int GAZING_LIGHT = 12;
	public static final int FORTUNES = 10;
	public static final int GAZE_TICKS = 40;
	public static final BooleanProperty GAZING = BooleanProperty.create("gazing");
	private static final VoxelShape SHAPE = Shapes.or(Block.box(4.0, 0.0, 4.0, 12.0, 3.0, 12.0), Block.box(4.5, 3.0, 4.5, 11.5, 10.0, 11.5));

	public CrystalBallBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(GAZING, false));
	}

	public static int light(BlockState state) {
		return state.getValue(GAZING) ? GAZING_LIGHT : LIGHT;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Gazing tells the gazer a fortune (unless the ball is still resting from the last one). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(GAZING)) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			int fortune = server.getRandom().nextInt(FORTUNES);
			server.setBlock(pos, state.setValue(GAZING, true), Block.UPDATE_ALL);
			server.scheduleTick(pos, this, GAZE_TICKS);
			server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
			server.gameEvent(player, GameEvent.BLOCK_ACTIVATE, pos);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.crystal_ball.fortune." + fortune));
		}
		return InteractionResult.SUCCESS;
	}

	/** The mist settles again. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(GAZING)) {
			level.setBlock(pos, state.setValue(GAZING, false), Block.UPDATE_ALL);
		}
	}

	/** Motes drift round the orb; a flurry while someone gazes. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		int motes = state.getValue(GAZING) ? 4 : random.nextInt(4) == 0 ? 1 : 0;
		for (int i = 0; i < motes; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5 + Math.cos(angle) * 0.35, pos.getY() + 0.45 + random.nextDouble() * 0.3,
					pos.getZ() + 0.5 + Math.sin(angle) * 0.35, 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(GAZING);
	}
}
