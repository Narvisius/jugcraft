package io.github.jimbozoomer.jugcraft.rocketry;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The rocket launcher (batch 41, docs/features/rocket-launcher.md): fires a rocket from the inventory (the other hand
 * first), straight and fast. A high-explosive rocket bursts hard where it hits; a homing rocket locks on to the hostile
 * mob nearest the crosshair, within {@link #HOMING_RANGE} blocks and in sight, and steers into it. Damage only: no
 * rocket breaks a block. One shot every two seconds.
 */
public class RocketLauncherItem extends Item {
	public static final int COOLDOWN = 40;
	public static final float SPEED = 3.0F;
	public static final int HOMING_RANGE = 48;
	/** How far from the crosshair (the cosine of the angle) a homing rocket will lock on: about 15 degrees. */
	private static final double LOCK_CONE = 0.966;
	public static final double HE_RADIUS = 5.0;
	public static final float HE_DAMAGE = 24.0F;
	public static final double HOMING_RADIUS = 3.5;
	public static final float HOMING_DAMAGE = 18.0F;

	public RocketLauncherItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack launcher = player.getItemInHand(hand);
		ItemStack ammo = findAmmo(player, hand);
		boolean free = player.hasInfiniteMaterials();
		if (ammo.isEmpty() && !free) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.rocket_launcher.empty"));
			}
			return InteractionResult.FAIL;
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS,
				2.0F, 0.5F);
		if (level instanceof ServerLevel server) {
			ItemStack round = ammo.isEmpty() ? new ItemStack(JugcraftRocketry.HE_ROCKET) : ammo.copyWithCount(1);
			CombatRocket rocket = Projectile.spawnProjectileFromRotation(CombatRocket::new, server, round, player, 0.0F, SPEED, 0.2F);
			if (rocket.homing()) {
				LivingEntity target = lockTarget(server, player);
				if (target != null) {
					rocket.lockOn(target);
				}
			}
		}
		player.getCooldowns().addCooldown(launcher, COOLDOWN);
		if (!free) {
			ammo.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	/** The hostile mob closest to the crosshair, within range and in sight, or null. */
	public static @Nullable LivingEntity lockTarget(ServerLevel level, Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		LivingEntity best = null;
		double bestAim = LOCK_CONE;
		for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(HOMING_RANGE),
				entity -> entity instanceof Enemy && entity.isAlive())) {
			Vec3 to = mob.getBoundingBox().getCenter().subtract(eye);
			double distance = to.length();
			if (distance > HOMING_RANGE || distance < 1.0e-3) {
				continue;
			}
			double aim = to.scale(1.0 / distance).dot(look);
			if (aim > bestAim && player.hasLineOfSight(mob)) {
				bestAim = aim;
				best = mob;
			}
		}
		return best;
	}

	/** A rocket in the other hand, or else the first one in the inventory; empty if there is none. */
	private static ItemStack findAmmo(Player player, InteractionHand hand) {
		ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
		if (isAmmo(other)) {
			return other;
		}
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (isAmmo(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static boolean isAmmo(ItemStack stack) {
		return stack.is(JugcraftRocketry.HE_ROCKET) || stack.is(JugcraftRocketry.HOMING_ROCKET);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.rocket_launcher").withStyle(ChatFormatting.GRAY));
	}
}
