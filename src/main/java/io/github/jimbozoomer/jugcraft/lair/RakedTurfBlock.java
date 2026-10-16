package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;

/**
 * Raked turf, the Alpha Werewolf's trail (docs/features/alpha-werewolf.md): the Moonlit Ring's floor torn by his claws as
 * he runs to ground (its moss, its trodden paths, the earth of its glades), so a trail of it leads from where he was to
 * the clearing he hides in. A lair fixture like the ring's others. It remembers what it was torn from ({@link #TURF}, in
 * the order of {@link #turfs}) and knits back to that by itself {@value #KNIT_TICKS} ticks after it is torn
 * (tools/alpha_werewolf.py RAKED), quietly: nothing round it is told, so the ferns and grass on it stand.
 */
public class RakedTurfBlock extends Block {
	public static final int KNIT_TICKS = 600;
	/** What it was torn from, in the order of {@link #turfs}. */
	public static final IntegerProperty TURF = IntegerProperty.create("turf", 0, 4);

	public RakedTurfBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TURF, 0));
	}

	/** The floors his claws tear: moss, a trodden path, coarse dirt, podzol and rooted dirt (tools/alpha_werewolf.py RAKED TURFS). */
	public static Block[] turfs() {
		return new Block[] {Blocks.MOSS_BLOCK, Blocks.DIRT_PATH, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.ROOTED_DIRT};
	}

	/** Raked turf torn from {@code floor}, or null if his claws do not tear it. */
	public static @Nullable BlockState tornFrom(Block raked, BlockState floor) {
		Block[] turfs = turfs();
		for (int i = 0; i < turfs.length; i++) {
			if (floor.is(turfs[i])) {
				return raked.defaultBlockState().setValue(TURF, i);
			}
		}
		return null;
	}

	/** What {@code state} knits back to. */
	public static BlockState knitsTo(BlockState state) {
		Block[] turfs = turfs();
		int turf = state.hasProperty(TURF) ? state.getValue(TURF) : 0;
		return turfs[Math.min(turf, turfs.length - 1)].defaultBlockState();
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		super.onPlace(state, level, pos, old, moved);
		if (!old.is(this)) {
			level.scheduleTick(pos, this, KNIT_TICKS);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.setBlock(pos, knitsTo(state), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TURF);
	}
}
