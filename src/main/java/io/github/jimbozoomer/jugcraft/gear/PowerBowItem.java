package io.github.jimbozoomer.jugcraft.gear;

import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.PoweredToolItem;
import io.github.jimbozoomer.jugcraft.tools.ToolUpgrades;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The power bow (batch 27, docs/features/gear-and-plastic.md). Charged, it fires arrows made of energy, at
 * {@link #ENERGY_PER_SHOT} JE a shot: no arrows needed, faster and harder-hitting than a bow's, and they cannot be
 * picked up. Uncharged, it is an ordinary bow that shoots the player's arrows.
 */
public class PowerBowItem extends BowItem implements Chargeable {
	public static final long CAPACITY = 100_000;
	public static final long ENERGY_PER_SHOT = 500;
	/** A full draw: the vanilla bow's arrows leave at 3.0. */
	public static final float ARROW_SPEED = 3.75F;
	/** Base damage before speed (the vanilla arrow's is 2.0). */
	public static final double ARROW_DAMAGE = 3.0;

	public PowerBowItem(Properties properties) {
		super(properties);
	}

	@Override
	public long baseCapacity() {
		return CAPACITY;
	}

	public static boolean charged(ItemStack stack) {
		return Chargeable.energy(stack) >= ENERGY_PER_SHOT;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (charged(player.getItemInHand(hand))) {
			player.startUsingItem(hand);
			return InteractionResult.CONSUME;
		}
		return super.use(level, player, hand);
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player) || !charged(stack)) {
			return super.releaseUsing(stack, level, entity, timeLeft);
		}
		float power = getPowerForTime(getUseDuration(stack, entity) - timeLeft);
		if (power < 0.1F) {
			return false;
		}
		if (level instanceof ServerLevel server) {
			Arrow arrow = new Arrow(server, player, new ItemStack(Items.ARROW), stack);
			arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * ARROW_SPEED, 1.0F);
			arrow.setBaseDamage(ARROW_DAMAGE);
			arrow.setCritArrow(power >= 1.0F);
			arrow.pickup = Arrow.Pickup.CREATIVE_ONLY;
			server.addFreshEntity(arrow);
			Chargeable.drain(stack, ENERGY_PER_SHOT);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
				1.0F, 1.4F + power * 0.5F);
		return true;
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
