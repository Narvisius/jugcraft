package io.github.jimbozoomer.jugcraft.lair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A breaker of the Foundry Trench (docs/features/foundry-trench.md): a knife switch on a steel cabinet at the foot of a
 * pylon, facing the pit ({@link #FACING}), with an amber lamp on top. Open or {@link #THROWN}; {@link #LIVE} while its
 * pylon is the one the Dreadnought's charge will strike (its lamp burns, light {@value #LIGHT}, tools/lairs.py BREAKER);
 * {@link #FOULED} while a plate of his hull jams it. A lair fixture: any player in the trench may use it. The
 * Dreadnought's fight listens ({@link #onUsed}) and throws a live breaker, or works a fouled one loose; while nothing
 * answers, using it only says that its pylon is dark. One player's uses are at least {@value #REST} ticks apart.
 */
public class DischargeBreakerBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty THROWN = BooleanProperty.create("thrown");
	public static final BooleanProperty LIVE = BooleanProperty.create("live");
	public static final BooleanProperty FOULED = BooleanProperty.create("fouled");
	public static final int LIGHT = 10;
	/** The least ticks between one player's uses of the breakers. */
	public static final int REST = 10;
	// The cabinet stands against the cell's back (the side away from the pit), its lamp on top, the switch before it.
	private static final VoxelShape NORTH = Block.box(2, 0, 5, 14, 16, 16);
	private static final VoxelShape SOUTH = Block.box(2, 0, 0, 14, 16, 11);
	private static final VoxelShape EAST = Block.box(0, 0, 2, 11, 16, 14);
	private static final VoxelShape WEST = Block.box(5, 0, 2, 16, 16, 14);
	private static final List<Used> USED = new ArrayList<>();
	private static final Map<UUID, Long> LAST_USE = new HashMap<>();

	/** Told when a player uses a breaker; true when it answered the use (threw it, or worked at its plate). */
	@FunctionalInterface
	public interface Used {
		boolean used(ServerLevel level, BlockPos pos, BlockState state, Player player);
	}

	/** Calls {@code listener} whenever a player uses a breaker (the Iron Dreadnought's fight listens). */
	public static void onUsed(Used listener) {
		USED.add(listener);
	}

	public DischargeBreakerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(THROWN, false).setValue(LIVE, false)
				.setValue(FOULED, false));
	}

	/** The light the breaker's lamp gives in {@code state}. */
	public static int light(BlockState state) {
		return state.getValue(LIVE) ? LIGHT : 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
			default -> NORTH;
		};
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		long now = server.getGameTime();
		Long last = LAST_USE.get(player.getUUID());
		if (last != null && now - last < REST && now >= last) {
			return InteractionResult.SUCCESS;
		}
		LAST_USE.put(player.getUUID(), now);
		for (Used listener : USED) {
			if (listener.used(server, pos, state, player)) {
				return InteractionResult.SUCCESS;
			}
		}
		// Nothing to route: the blade only sparks in its jaw.
		server.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 0.6F);
		server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.02);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.breaker.dead"));
		return InteractionResult.SUCCESS;
	}

	/** Sets one of the breaker's properties at {@code pos}; whether it was a breaker and it changed. */
	public static boolean set(Level level, BlockPos pos, BooleanProperty property, boolean value) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof DischargeBreakerBlock) || state.getValue(property) == value) {
			return false;
		}
		level.setBlock(pos, state.setValue(property, value), Block.UPDATE_ALL);
		return true;
	}

	/** Forgets who used the breakers when (the server stopping). */
	static void forget() {
		LAST_USE.clear();
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIVE) && random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.0,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.02, 0.0);
		}
		if (state.getValue(FOULED) && random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, THROWN, LIVE, FOULED);
	}
}
