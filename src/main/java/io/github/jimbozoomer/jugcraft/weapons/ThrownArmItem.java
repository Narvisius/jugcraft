package io.github.jimbozoomer.jugcraft.weapons;

import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A thrown arm (Arms VIII, batch 59, docs/features/arms-viii.md; {@link JugcraftArms#THROWN}): an arm of its kind in the
 * hand, thrown as vanilla's trident is. Hold use to wind it back (the trident's pose); let go after at least its wind to
 * throw it, as a {@link ThrownArm} carrying the arm itself, enchantments and wear and all. A throw wears it by
 * {@link JugcraftArms#THROW_WEAR} (never breaking it) and holds the next throw of the same arm back
 * {@link JugcraftArms#THROW_COOLDOWN} ticks. A creative player keeps theirs and throws a copy, which does not come down
 * as an item. Everything is decided on the server.
 */
public class ThrownArmItem extends ArmItem {
	private final JugcraftArms.Thrown thrown;

	public ThrownArmItem(String kind, JugcraftArms.Thrown thrown, Properties properties) {
		super(kind, properties);
		this.thrown = thrown;
	}

	/** How this arm flies (its kind's, in its metal). */
	public JugcraftArms.Thrown thrown() {
		return thrown;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("tooltip.jugcraft.arms.throw." + kind(),
				String.format(Locale.ROOT, "%.0f", thrown.damage())).withStyle(ChatFormatting.GOLD));
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.TRIDENT;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	/** Use starts the wind (from either hand); the throw is on letting go ({@link #releaseUsing}). */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.nextDamageWillBreak()) {
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player) || getUseDuration(stack, entity) - timeLeft < thrown.wind() || stack.nextDamageWillBreak()) {
			return false;
		}
		if (level instanceof ServerLevel server) {
			stack.hurtWithoutBreaking(JugcraftArms.THROW_WEAR, player);
			ThrownArm arm = Projectile.spawnProjectileFromRotation(ThrownArm::new, server, stack, player, 0.0F, thrown.speed(), 1.0F);
			if (player.hasInfiniteMaterials()) {
				arm.fromCreative();
			} else {
				player.getInventory().removeItem(stack);
			}
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F,
					kind().equals("chakram") ? 1.4F : 1.0F);
		}
		player.getCooldowns().addCooldown(stack, JugcraftArms.THROW_COOLDOWN);
		player.awardStat(Stats.ITEM_USED.get(this));
		return true;
	}
}
