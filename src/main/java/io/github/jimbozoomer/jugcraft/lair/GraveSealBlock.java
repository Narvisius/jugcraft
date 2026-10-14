package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Rune;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A grave seal of the Archive of Last Names (docs/features/crypt-archive.md): a disc of red wax on an iron plate on an
 * alcove's back wall, pressed with the rune of the name over it ({@link Rune}), whole or {@link #BROKEN}. A lair fixture:
 * any player in the archive may use it. The Crypt Lich's fight listens ({@link #onUsed}) and breaks a seal whose name is
 * lit; while nothing binds it, using it only says that it is cold. One player's uses are at least {@value #REST} ticks
 * apart. {@link #FACING} is the way its face looks, out of the wall.
 */
public class GraveSealBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<Rune> RUNE = NameStoneBlock.RUNE;
	public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
	/** The least ticks between one player's uses of the seals. */
	public static final int REST = 10;
	private static final VoxelShape NORTH = Block.box(2, 2, 14, 14, 14, 16);
	private static final VoxelShape SOUTH = Block.box(2, 2, 0, 14, 14, 2);
	private static final VoxelShape EAST = Block.box(0, 2, 2, 2, 14, 14);
	private static final VoxelShape WEST = Block.box(14, 2, 2, 16, 14, 14);
	private static final List<Used> USED = new ArrayList<>();
	private static final Map<UUID, Long> LAST_USE = new HashMap<>();

	/** Told when a player uses a grave seal; true when it answered the use (broke the seal, or raised the dead). */
	@FunctionalInterface
	public interface Used {
		boolean used(ServerLevel level, BlockPos pos, BlockState state, Player player);
	}

	/** Calls {@code listener} whenever a player uses a grave seal (the Crypt Lich's fight listens). */
	public static void onUsed(Used listener) {
		USED.add(listener);
	}

	public GraveSealBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNE, Rune.SPADE).setValue(BROKEN, false));
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
		player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.seal.cold"));
		return InteractionResult.SUCCESS;
	}

	/** Breaks or mends the seal at {@code pos}; whether it was a seal that changed. */
	public static boolean setBroken(Level level, BlockPos pos, boolean broken) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof GraveSealBlock) || state.getValue(BROKEN) == broken) {
			return false;
		}
		level.setBlock(pos, state.setValue(BROKEN, broken), Block.UPDATE_ALL);
		return true;
	}

	/** Forgets who used the seals when (the server stopping). */
	static void forget() {
		LAST_USE.clear();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, RUNE, BROKEN);
	}
}
