package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The grenade launcher: fires a grenade of any kind from the inventory (the other hand first) two and a half times as
 * fast as a throw, so it flies flatter and further. One every 1.5 seconds.
 */
public class GrenadeLauncherItem extends Item {
	public static final float LAUNCH_SPEED = 2.5F;
	public static final int COOLDOWN = 30;

	public GrenadeLauncherItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack launcher = player.getItemInHand(hand);
		ItemStack ammo = findAmmo(player, hand);
		boolean free = player.hasInfiniteMaterials();
		if (ammo.isEmpty() && !free) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.grenade_launcher.empty"));
			}
			return InteractionResult.FAIL;
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS,
				1.0F, 0.6F);
		if (level instanceof ServerLevel server) {
			ItemStack round = ammo.isEmpty() ? new ItemStack(PetroItems.GRENADE) : ammo;
			Projectile.spawnProjectileFromRotation(GrenadeEntity::new, server, round, player, 0.0F, LAUNCH_SPEED, 0.5F);
		}
		player.getCooldowns().addCooldown(launcher, COOLDOWN);
		if (!free) {
			ammo.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	/** A grenade of any kind in the other hand, or else the first one in the inventory; empty if there is none. */
	private static ItemStack findAmmo(Player player, InteractionHand hand) {
		ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
		if (other.getItem() instanceof GrenadeItem) {
			return other;
		}
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.getItem() instanceof GrenadeItem) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}
}
