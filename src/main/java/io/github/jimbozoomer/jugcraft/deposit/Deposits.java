package io.github.jimbozoomer.jugcraft.deposit;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * How much is left in each surface deposit block. Every deposit block starts with {@link #CAPACITY} units; nothing is
 * stored until a drill takes from it, then how much it has given is saved with the dimension. A block that has given
 * everything turns to stone. Breaking a deposit block by hand gives nothing and forgets it.
 */
public final class Deposits {
	/** Units in a fresh deposit block; one unit is one coal or one raw ore. */
	public static final int CAPACITY = 1_000;

	private Deposits() {
	}

	/** Units left in the deposit block at {@code pos} (0 when it is not a deposit). */
	public static int remaining(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockState(pos).getBlock() instanceof DepositBlock)) {
			return 0;
		}
		return Math.max(0, CAPACITY - data(level).taken(pos));
	}

	/**
	 * Takes up to {@code units} from the deposit block at {@code pos} and returns how many it gave. When the last unit
	 * goes, the block becomes stone.
	 */
	public static int extract(ServerLevel level, BlockPos pos, int units) {
		int left = remaining(level, pos);
		int got = Math.min(units, left);
		if (got <= 0) {
			return 0;
		}
		if (got == left) {
			level.setBlock(pos, depleted(level.getBlockState(pos)), 3); // Removal forgets the block (see DepositBlock).
		} else {
			data(level).take(pos, got);
		}
		return got;
	}

	/** What a used-up deposit leaves behind. */
	static BlockState depleted(BlockState deposit) {
		return Blocks.STONE.defaultBlockState();
	}

	/** Called when a deposit block is removed, so a new one placed there later starts full. */
	static void forget(ServerLevel level, BlockPos pos) {
		data(level).forget(pos);
	}

	private static Data data(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(Data.TYPE);
	}

	/** Units taken from each touched deposit block, saved with the dimension (data/jugcraft_deposits.dat). */
	static final class Data extends SavedData {
		private static final Codec<Map<Long, Integer>> MAP = Codec.unboundedMap(
				Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.INT);
		static final Codec<Data> CODEC = MAP.xmap(Data::new, data -> data.taken);
		static final SavedDataType<Data> TYPE = new SavedDataType<>(Jugcraft.id("deposits"), Data::new, CODEC, null);

		private final Map<Long, Integer> taken;

		Data() {
			this(Map.of());
		}

		Data(Map<Long, Integer> taken) {
			this.taken = new HashMap<>(taken);
		}

		int taken(BlockPos pos) {
			return taken.getOrDefault(pos.asLong(), 0);
		}

		void take(BlockPos pos, int units) {
			taken.merge(pos.asLong(), units, Integer::sum);
			setDirty();
		}

		void forget(BlockPos pos) {
			if (taken.remove(pos.asLong()) != null) {
				setDirty();
			}
		}
	}
}
