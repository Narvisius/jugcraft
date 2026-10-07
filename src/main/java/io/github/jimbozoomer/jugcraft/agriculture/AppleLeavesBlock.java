package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Apple tree leaves ({@link FruitingLeavesBlock}): they blossom ({@link #FRUIT} 1) and then hang with ripe red apples
 * ({@link #FRUIT} 2), one step in {@link #FRUIT_CHANCE} random ticks, about a Minecraft day in all. A right-click picks
 * them: 1-3 vanilla apples drop. Broken, they drop apple seeds now and then, sticks, and apples as oak leaves do.
 */
public class AppleLeavesBlock extends FruitingLeavesBlock {
	/** One fruit stage in this many random ticks. Keep in sync with tools/agriculture.py. */
	public static final int FRUIT_CHANCE = 10;
	public static final int PICK_MIN = 1;
	public static final int PICK_MAX = 3;

	public AppleLeavesBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected int fruitChance() {
		return FRUIT_CHANCE;
	}

	@Override
	protected ItemStack fruit(RandomSource random) {
		return new ItemStack(Items.APPLE, PICK_MIN + random.nextInt(PICK_MAX - PICK_MIN + 1));
	}
}
