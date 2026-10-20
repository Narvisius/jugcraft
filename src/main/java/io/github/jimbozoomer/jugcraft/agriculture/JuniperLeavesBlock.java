package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Evergreen juniper, including grounded bushes that set berries along their exposed edges. */
public final class JuniperLeavesBlock extends FruitingLeavesBlock {
	public JuniperLeavesBlock(Properties properties) { super(properties); }
	@Override protected int fruitChance() { return 10; }
	@Override protected ItemStack fruit(RandomSource random) {
		return new ItemStack(JugcraftAgriculture.item("juniper_berries"), 1 + random.nextInt(2));
	}
	@Override protected boolean hasFruitSpace(Level level, BlockPos pos) {
		return super.hasFruitSpace(level, pos) || exposedSide(level, pos) != null;
	}
	@Override protected BlockPos fruitDropPos(Level level, BlockPos pos) {
		BlockPos side = level.isEmptyBlock(pos.below()) ? null : exposedSide(level, pos);
		return side == null ? super.fruitDropPos(level, pos) : side;
	}
	private static BlockPos exposedSide(Level level, BlockPos pos) {
		for (var direction : Direction.Plane.HORIZONTAL) {
			BlockPos side = pos.relative(direction);
			if (level.isEmptyBlock(side)) return side;
		}
		return null;
	}
	@Override protected boolean isRandomlyTicking(BlockState state) {
		return !state.getValue(PERSISTENT) && (state.getValue(DISTANCE) == DECAY_DISTANCE || state.getValue(FRUIT) < RIPE);
	}
}
