package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/** Four small openings under a one-stem elder crown let its leaves bear fruit over air. */
public final class FruitingShrubDecorator extends TreeDecorator {
	public static final FruitingShrubDecorator INSTANCE = new FruitingShrubDecorator();
	public static final MapCodec<FruitingShrubDecorator> CODEC = MapCodec.unit(INSTANCE);
	public static final TreeDecoratorType<FruitingShrubDecorator> TYPE = new TreeDecoratorType<>(CODEC);

	private FruitingShrubDecorator() { }
	@Override protected TreeDecoratorType<?> type() { return TYPE; }

	@Override public void place(Context context) {
		// The native trunk placement list can also include the dirt placed beneath it.
		var stems = context.logs().stream().filter(pos -> context.level().isStateAtPosition(pos,
				state -> state.is(Blocks.OAK_LOG))).toList();
		if (stems.size() != 1) return;
		BlockPos stem = stems.getFirst();
		var leaves = JugcraftAgriculture.block("elder_leaves");
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos gap = stem.relative(side, 2);
			// Only prune foliage created by this tree; never soil, a log or a neighbouring build.
			if (context.leaves().contains(gap) && context.leaves().contains(gap.above())
					&& context.level().isStateAtPosition(gap, state -> state.is(leaves))
					&& context.level().isStateAtPosition(gap.above(), state -> state.is(leaves))) {
				context.setBlock(gap, Blocks.AIR.defaultBlockState());
			}
		}
	}
}
