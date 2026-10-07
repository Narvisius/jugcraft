package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.gear.TraitTooltips;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A longbow (Arms VI, batch 55, docs/features/arms-vi.md; {@link JugcraftArms#RANGED}): vanilla's bow, drawn over the
 * longbow's own draw on vanilla's curve, loosing its arrow at the longbow's speed with the longbow's base damage. Ammo,
 * enchantments (Power, Punch, Flame, Infinity) and the shot itself are vanilla's, through
 * ProjectileWeaponItem's draw and shoot, so arrows are used and picked up as a bow's are.
 */
public class ArmBowItem extends BowItem {
	/** Vanilla's bow looses a fully drawn arrow at this speed (blocks a tick). */
	public static final float VANILLA_SPEED = 3.0F;
	private final JugcraftArms.Ranged ranged;

	public ArmBowItem(JugcraftArms.Ranged ranged, Properties properties) {
		super(properties);
		this.ranged = ranged;
	}

	public JugcraftArms.Ranged ranged() {
		return ranged;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		// Its trait, with what it is and its numbers as the description (docs/features/trait-details.md).
		String key = "tooltip.jugcraft.arms." + ranged.name();
		TraitTooltips traits = TraitTooltips.of(tooltip);
		traits.trait(key + ".trait", ChatFormatting.YELLOW, Component.translatable(key),
				Component.translatable("tooltip.jugcraft.arms.longbow.stats", String.format(Locale.ROOT, "%.1f", ranged.draw() / 20.0F),
						Math.round((ranged.speed() / VANILLA_SPEED - 1.0F) * 100.0F)));
		traits.end();
	}

	/** How far drawn a longbow is after this many ticks: vanilla's curve stretched over its draw (1 is a full draw). */
	public static float power(int ticks, int draw) {
		float f = (float) ticks / draw;
		f = (f * f + f * 2.0F) / 3.0F;
		return Math.min(f, 1.0F);
	}

	/** As vanilla's bow, with the longbow's draw and speed. */
	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player)) {
			return false;
		}
		ItemStack ammo = player.getProjectile(stack);
		if (ammo.isEmpty()) {
			return false;
		}
		float power = power(getUseDuration(stack, entity) - timeLeft, ranged.draw());
		if (power < 0.1F) {
			return false;
		}
		List<ItemStack> drawn = draw(stack, ammo, player);
		if (level instanceof ServerLevel server && !drawn.isEmpty()) {
			shoot(server, player, player.getUsedItemHand(), stack, drawn, power * ranged.speed(), 1.0F, power == 1.0F, null);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
				1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
		player.awardStat(Stats.ITEM_USED.get(this));
		return true;
	}

	/** Vanilla's arrow, with the longbow's base damage. */
	@Override
	protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean crit) {
		Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, crit);
		if (projectile instanceof AbstractArrow arrow) {
			arrow.setBaseDamage(ranged.damage());
		}
		return projectile;
	}
}
