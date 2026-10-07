package io.github.jimbozoomer.jugcraft.agriculture;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The hot-air balloon fiesta (fall addition 29): using a balloon. With fuel in hand it goes into the tanks; sneaking
 * with an empty hand packs an empty balloon on the ground back into its item; otherwise a player climbs into the basket,
 * if there is room. Decided on the server, after its reach check; the client only swings the arm.
 */
public final class Balloons {
	private Balloons() {
	}

	static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> use(player, level, hand, entity));
		BalloonControlPayload.register();
	}

	public static InteractionResult use(Player player, Level level, InteractionHand hand, Entity entity) {
		if (!(entity instanceof HotAirBalloon balloon) || player.isSpectator() || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer user)) {
			return InteractionResult.SUCCESS;
		}
		if (HotAirBalloon.fuelUnits(held) > 0) {
			return balloon.refuel(held, user) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		if (held.isEmpty() && user.isSecondaryUseActive()) {
			if (balloon.isVehicle() || !balloon.grounded()) {
				return InteractionResult.PASS;
			}
			ItemStack item = balloon.item();
			if (!user.getInventory().add(item)) {
				user.spawnAtLocation(server, item);
			}
			balloon.discard();
			return InteractionResult.SUCCESS;
		}
		if (user.isPassenger()) {
			return InteractionResult.PASS;
		}
		if (!user.startRiding(balloon)) {
			user.sendOverlayMessage(Component.translatable("message.jugcraft.balloon.full"));
			return InteractionResult.FAIL;
		}
		return InteractionResult.SUCCESS;
	}
}
