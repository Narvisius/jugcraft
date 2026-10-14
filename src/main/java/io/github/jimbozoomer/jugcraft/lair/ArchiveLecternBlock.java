package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Glow;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Lectern of Last Names (docs/features/crypt-archive.md): the archive's great book open on a lectern of bone at the
 * foot of its stair. A lair fixture: any player in the archive may read it. Reading it reads the names back: any of the
 * three name-stones that has been scraped blank ({@link Glow#ERASED}) is cut again (dark), and the reader is told whose;
 * if none was, the reader is told the three names. The Crypt Lich's fight is told who read which names back
 * ({@link #onRead}). The name-stones are found where the archive's layout puts them, from where the lectern stands
 * ({@link CryptArchive}). One player's readings are at least {@value #REST} ticks apart (tools/lairs.py LECTERN).
 */
public class ArchiveLecternBlock extends HorizontalDirectionalBlock {
	/** The least ticks between one player's readings. */
	public static final int REST = 20;
	private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 2, 13), Block.box(6, 2, 6, 10, 11, 10),
			Block.box(1, 11, 1, 15, 14, 15));
	private static final List<Read> READ = new ArrayList<>();
	private static final Map<UUID, Long> LAST_READ = new HashMap<>();

	/** Told when a player reads the lectern, with the names it cut again (empty when none had been scraped). */
	@FunctionalInterface
	public interface Read {
		void read(ServerLevel level, BlockPos lectern, Player player, List<Rune> restored);
	}

	/** Calls {@code listener} whenever a player reads the Lectern of Last Names (the Crypt Lich's fight listens). */
	public static void onRead(Read listener) {
		READ.add(listener);
	}

	public ArchiveLecternBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		long now = server.getGameTime();
		Long last = LAST_READ.get(player.getUUID());
		if (last != null && now - last < REST && now >= last) {
			return InteractionResult.SUCCESS;
		}
		LAST_READ.put(player.getUUID(), now);
		read(server, pos, player);
		return InteractionResult.SUCCESS;
	}

	/** {@code player} reads the lectern at {@code pos}: the scraped names are cut again. Returns the names restored. */
	public static List<Rune> read(ServerLevel level, BlockPos pos, Player player) {
		List<Rune> restored = new ArrayList<>();
		for (Rune rune : Rune.values()) {
			BlockPos stone = CryptArchive.stoneFrom(pos, rune);
			BlockState state = level.getBlockState(stone);
			if (state.getBlock() instanceof NameStoneBlock && state.getValue(NameStoneBlock.GLOW) == Glow.ERASED
					&& NameStoneBlock.setGlow(level, stone, Glow.DARK)) {
				restored.add(rune);
				level.playSound(null, stone, SoundEvents.DEEPSLATE_BRICKS_HIT, SoundSource.BLOCKS, 1.0F, 0.7F);
			}
		}
		level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.8F);
		if (restored.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.lectern.read", Rune.SPADE.display(), Rune.CROWN.display(),
					Rune.QUILL.display()));
		} else {
			for (Rune rune : restored) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.lectern.restored", rune.display()));
			}
		}
		for (Read listener : READ) {
			listener.read(level, pos, player, restored);
		}
		return restored;
	}

	/** Forgets who read when (the server stopping). */
	static void forget() {
		LAST_READ.clear();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
