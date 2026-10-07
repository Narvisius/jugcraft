package io.github.jimbozoomer.jugcraft.gear;

import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.PoweredToolItem;
import io.github.jimbozoomer.jugcraft.tools.ToolUpgrades;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The power katana (batch 27, docs/features/gear-and-plastic.md): a sword that runs on JE instead of wearing out. Each
 * hit costs {@link #ENERGY_PER_HIT}; with less than that it hits for 1, like a bare hand. Charged at the charging
 * station, like the powered tools.
 */
public class PowerKatanaItem extends Item implements Chargeable {
	public static final long CAPACITY = 200_000;
	public static final long ENERGY_PER_HIT = 1_000;

	public PowerKatanaItem(Properties properties) {
		super(properties);
	}

	@Override
	public long baseCapacity() {
		return CAPACITY;
	}

	public static boolean charged(ItemStack stack) {
		return Chargeable.energy(stack) >= ENERGY_PER_HIT;
	}

	/** Uncharged, the blade's damage is taken back down to 1. */
	@Override
	public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
		ItemStack weapon = source.getWeaponItem();
		if (weapon != null && weapon.getItem() == this && !charged(weapon)) {
			return Math.min(0.0F, 1.0F - damage);
		}
		return 0.0F;
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		Chargeable.drain(stack, ENERGY_PER_HIT);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return PoweredToolItem.barWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return PoweredToolItem.BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(PoweredToolItem.energyLine(stack));
		ToolUpgrades.appendTooltip(stack, tooltip);
	}
}
