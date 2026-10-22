package io.github.jimbozoomer.jugcraft.finance;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Keeps other players out of claimed land: breaking, placing, using blocks (doors, chests, machines) and hitting or
 * using entities (animals, item frames, armour stands). Owners, players they trust and operators are let through.
 * Explosions/fire are shielded and fluid/piston ownership boundaries are enforced by the native mixins.
 * Explicit party sharing is checked through LandRegistry.
 */
final class LandProtection {
	private LandProtection() {
	}

	static void register() {
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, source, amount) ->
                target instanceof Player || !(source.getEntity() instanceof Player player)
                || LandRegistry.mayBuild(player, target.level(), target.blockPosition()));
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> allowed(player, level, pos));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				allowed(player, level, pos) ? InteractionResult.PASS : InteractionResult.FAIL);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			BlockPos clicked = hit.getBlockPos();
			// Also the block beside it, where a placed block would go.
			return allowed(player, level, clicked) && allowed(player, level, clicked.relative(hit.getDirection()))
					? InteractionResult.PASS : InteractionResult.FAIL;
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				entity instanceof Player || allowed(player, level, entity.blockPosition()) ? InteractionResult.PASS : InteractionResult.FAIL);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				entity instanceof Player || allowed(player, level, entity.blockPosition()) ? InteractionResult.PASS : InteractionResult.FAIL);
	}

	private static boolean allowed(Player player, Level level, BlockPos pos) {
		if (LandRegistry.mayBuild(player, level, pos)) {
			return true;
		}
		if (player instanceof ServerPlayer server) {
			server.sendOverlayMessage(Component.literal("This land belongs to " + LandRegistry.ownerAt(level, pos) + "."));
		}
		return false;
	}
}
