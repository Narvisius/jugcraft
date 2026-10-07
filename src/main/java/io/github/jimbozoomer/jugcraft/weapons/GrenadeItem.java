package io.github.jimbozoomer.jugcraft.weapons;

import java.util.function.Consumer;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A grenade, thrown by hand like a snowball but heavier: it goes off where it lands, as its {@link Warhead} says (a
 * plain grenade is a frag grenade). One a second. The grenade launcher fires any of them.
 */
public class GrenadeItem extends Item {
	public static final float THROW_SPEED = 1.0F;
	public static final int COOLDOWN = 20;

	private final Warhead warhead;

	public GrenadeItem(Properties properties) {
		this(properties, Warhead.FRAG);
	}

	public GrenadeItem(Properties properties, Warhead warhead) {
		super(properties.stacksTo(16));
		this.warhead = warhead;
	}

	public Warhead warhead() {
		return warhead;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		DescribedItem.describe(stack, tooltip);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
				0.6F, 0.5F);
		if (level instanceof ServerLevel server) {
			Projectile.spawnProjectileFromRotation(GrenadeEntity::new, server, stack, player, 0.0F, THROW_SPEED, 1.0F);
		}
		player.getCooldowns().addCooldown(stack, COOLDOWN);
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
