package io.github.jimbozoomer.jugcraft.agriculture;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The piñata party (fall addition 28): using a hung piñata. With anything in hand but the Piñata Stick, it goes in
 * (the whole stack), while there is room; sneaking with an empty hand, whoever hung it takes it down. Decided on the
 * server; the client only swings the arm.
 */
public final class Pinatas {
	public static final String STICK = "pinata_stick";
	public static final String BLINDFOLD = "blindfold";

	private Pinatas() {
	}

	static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> use(player, level, hand, entity));
	}

	public static InteractionResult use(Player player, Level level, InteractionHand hand, Entity entity) {
		if (!(entity instanceof Pinata pinata) || player.isSpectator()) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		boolean takeDown = held.isEmpty() && player.isSecondaryUseActive();
		boolean filling = !held.isEmpty() && !held.is(JugcraftAgriculture.item(STICK));
		if (!takeDown && !filling) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer user)) {
			return InteractionResult.SUCCESS;
		}
		if (takeDown) {
			if (!pinata.ownedBy(user)) {
				return InteractionResult.FAIL;
			}
			pinata.takeDown(server, user);
			return InteractionResult.SUCCESS;
		}
		if (!pinata.fill(held)) {
			user.sendOverlayMessage(Component.translatable("message.jugcraft.pinata.full"));
			return InteractionResult.FAIL;
		}
		user.sendOverlayMessage(Component.translatable("message.jugcraft.pinata.filled", held.getHoverName()));
		server.playSound(null, pinata.getX(), pinata.getY() + 0.5, pinata.getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0F, 1.2F);
		if (!user.hasInfiniteMaterials()) {
			held.setCount(0);
		}
		return InteractionResult.SUCCESS;
	}
}
