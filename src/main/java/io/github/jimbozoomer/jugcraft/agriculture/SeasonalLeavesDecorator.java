package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * Gives a generated tree's seasonal leaves today's look as the tree is placed ({@code "decorators": [{"type":
 * "jugcraft:seasonal_leaves"}]} in each seasonal tree feature). World generation places blocks without
 * {@link SeasonalLeavesBlock#onPlace}, so without this a tree would stay green until a random tick reached it, and trees
 * that stand apart (aspens, for example) would each wait for their own. Other leaves are left alone.
 */
public final class SeasonalLeavesDecorator extends TreeDecorator {
	public static final SeasonalLeavesDecorator INSTANCE = new SeasonalLeavesDecorator();
	public static final MapCodec<SeasonalLeavesDecorator> CODEC = MapCodec.unit(INSTANCE);
	public static final TreeDecoratorType<SeasonalLeavesDecorator> TYPE = new TreeDecoratorType<>(CODEC);

	private SeasonalLeavesDecorator() {
	}

	@Override
	protected TreeDecoratorType<?> type() {
		return TYPE;
	}

	@Override
	public void place(TreeDecorator.Context context) {
		int day = JugcraftSeasons.today();
		BlockState[] found = new BlockState[1];
		for (BlockPos pos : context.leaves()) {
			found[0] = null;
			context.level().isStateAtPosition(pos, state -> {
				found[0] = state;
				return true;
			});
			if (found[0] != null && found[0].getBlock() instanceof SeasonalLeavesBlock leaves) {
				SeasonalLeavesBlock.Foliage look = leaves.schedule().on(day, pos);
				if (found[0].getValue(SeasonalLeavesBlock.SEASON) != look) {
					context.setBlock(pos, found[0].setValue(SeasonalLeavesBlock.SEASON, look));
				}
			}
		}
	}
}
