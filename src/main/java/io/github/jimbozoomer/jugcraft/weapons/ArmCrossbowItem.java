package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.gear.TraitTooltips;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * An arbalest (Arms VI, batch 55, docs/features/arms-vi.md; {@link JugcraftArms#RANGED}): vanilla's crossbow, loading,
 * holding and firing as it does (with its enchantments), but shooting its bolts faster, at the arbalest's speed, with the
 * arbalest's base damage. Firework rockets fly as from vanilla's crossbow.
 */
public class ArmCrossbowItem extends CrossbowItem {
	/** Vanilla's arrows' base damage. */
	public static final float VANILLA_DAMAGE = 2.0F;
	private final JugcraftArms.Ranged ranged;

	public ArmCrossbowItem(JugcraftArms.Ranged ranged, Properties properties) {
		super(properties);
		this.ranged = ranged;
	}

	public JugcraftArms.Ranged ranged() {
		return ranged;
	}

	/** As vanilla's crossbow's tooltip (what it is loaded with), then what the arbalest is and how it compares. */
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		// Its trait, with what it is and its numbers as the description (docs/features/trait-details.md).
		String key = "tooltip.jugcraft.arms." + ranged.name();
		TraitTooltips traits = TraitTooltips.of(tooltip);
		traits.trait(key + ".trait", ChatFormatting.YELLOW, Component.translatable(key),
				Component.translatable("tooltip.jugcraft.arms.arbalest.stats",
						Math.round((ranged.speed() / JugcraftArms.CROSSBOW_SPEED - 1.0F) * 100.0F),
						Math.round((ranged.damage() / VANILLA_DAMAGE - 1.0F) * 100.0F)));
		traits.end();
	}

	@Override
	protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle,
			LivingEntity target) {
		float speed = projectile instanceof AbstractArrow ? velocity * ranged.speed() / JugcraftArms.CROSSBOW_SPEED : velocity;
		super.shootProjectile(shooter, projectile, index, speed, inaccuracy, angle, target);
	}

	@Override
	protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean crit) {
		Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, crit);
		if (projectile instanceof AbstractArrow arrow) {
			arrow.setBaseDamage(ranged.damage());
		}
		return projectile;
	}
}
