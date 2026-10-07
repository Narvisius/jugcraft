package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Chestnut tree leaves ({@link FruitingLeavesBlock}): they grow a spiny bur ({@link #FRUIT} 1) that ripens and splits open
 * ({@link #FRUIT} 2), one step in {@link #FRUIT_CHANCE} random ticks, about a Minecraft day in all. A right-click picks a
 * ripe bur: 1-2 chestnuts drop. Broken, they drop chestnuts and sticks.
 */
public class ChestnutLeavesBlock extends FruitingLeavesBlock {
	/** One fruit stage in this many random ticks. Keep in sync with tools/agriculture.py. */
	public static final int FRUIT_CHANCE = 10;
	public static final int PICK_MIN = 1;
	public static final int PICK_MAX = 2;

	public ChestnutLeavesBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected int fruitChance() {
		return FRUIT_CHANCE;
	}

	@Override
	protected ItemStack fruit(RandomSource random) {
		return new ItemStack(JugcraftAgriculture.item("chestnut"), PICK_MIN + random.nextInt(PICK_MAX - PICK_MIN + 1));
	}
}
