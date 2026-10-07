package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A mooncake, baked in the Cooking Pot. Eaten outdoors on a full-moon night (the first night of the moon's eight on the
 * overworld clock), it also gives Luck for {@value #LUCK_TICKS} ticks.
 */
public class MooncakeItem extends Item {
	public static final int LUCK_TICKS = 6000;
	/** Night on the overworld clock: from dusk to dawn. */
	public static final int NIGHT_START = 13000;
	public static final int NIGHT_END = 23000;
	private static final int DAY = 24000;

	public MooncakeItem(Properties properties) {
		super(properties);
	}

	/** Whether {@code dayTime} on the overworld clock is a full-moon night. */
	public static boolean fullMoonNight(long dayTime) {
		long hour = Math.floorMod(dayTime, (long) DAY);
		return Math.floorMod(dayTime / DAY, 8L) == 0 && hour >= NIGHT_START && hour < NIGHT_END;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		boolean blessed = !level.isClientSide() && entity instanceof Player && fullMoonNight(level.getOverworldClockTime())
				&& level.canSeeSky(entity.blockPosition().above());
		ItemStack left = super.finishUsingItem(stack, level, entity);
		if (blessed) {
			entity.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_TICKS));
		}
		return left;
	}
}
