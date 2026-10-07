package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * A sky lantern: used, it is lit and let go just in front of its holder as a {@link SkyLantern}, in its colour (dye it
 * in the crafting grid, as leather armour is dyed) and carrying its name as a wish (name it in an anvil). Every release
 * counts towards a lantern festival ({@link SkyLanterns}).
 */
public class SkyLanternItem extends Item {
	public SkyLanternItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server) {
			Vec3 at = player.getEyePosition().add(player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(0.8)).add(0.0, 0.2, 0.0);
			release(server, at, stack);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.6F, 1.2F);
			level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Lets a lantern made from {@code stack} go at {@code at}; returns it. */
	public static SkyLantern release(ServerLevel level, Vec3 at, ItemStack stack) {
		DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
		SkyLantern lantern = new SkyLantern(level, at, dyed == null ? SkyLantern.DEFAULT_COLOUR : dyed.rgb());
		if (stack.has(DataComponents.CUSTOM_NAME)) {
			lantern.setCustomName(stack.getHoverName());
			lantern.setCustomNameVisible(true);
		}
		level.addFreshEntity(lantern);
		SkyLanterns.released(level, at);
		return lantern;
	}
}
