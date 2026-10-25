package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal;
import io.github.jimbozoomer.jugcraft.menagerie.RecoveredCreaturePart;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Keeps other players out of claimed land: breaking, placing, using blocks (doors, chests, machines) and hitting or
 * using entities (animals, item frames, armour stands). Wild hostile mobs may still be fought by visitors.
 * Owners, players they trust and operators are let through.
 * Explosions/fire are shielded and fluid/piston ownership boundaries are enforced by the native mixins.
 * Explicit party sharing is checked through LandRegistry.
 */
final class LandProtection {
	private LandProtection() {
	}

	static void register() {
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, source, amount) ->
                mayFightWithoutClaimAccess(target) || !(source.getEntity() instanceof Player player)
                || LandRegistry.mayBuild(player, target.level(), target.blockPosition()));
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> allowed(player, level, pos));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				allowed(player, level, pos) ? InteractionResult.PASS : InteractionResult.FAIL);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			BlockPos clicked = hit.getBlockPos();
			// Published vendors perform their own bounded trade/menu action. Never grant build/container access.
			var shopUse=io.github.jimbozoomer.jugcraft.jimtafacts.industry.PersonalWorkshops.publicUse(player,level,clicked);
			if(shopUse!=InteractionResult.PASS)return shopUse;
			// Also the block beside it, where a placed block would go.
			return allowed(player, level, clicked) && allowed(player, level, clicked.relative(hit.getDirection()))
					? InteractionResult.PASS : InteractionResult.FAIL;
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				entity instanceof Player || allowed(player, level, entity.blockPosition()) ? InteractionResult.PASS : InteractionResult.FAIL);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			// Parts forward damage to their parent; its location and ownership govern the hit.
			Entity target = entity;
			if (target instanceof RecoveredCreaturePart part) target = part.parent();
			else if (target instanceof EnderDragonPart part) target = part.parentMob;
			return target != null && (mayFightWithoutClaimAccess(target) || allowed(player, level, target.blockPosition()))
					? InteractionResult.PASS : InteractionResult.FAIL;
		});
	}

	/** Only combat is exempt; using an entity still requires ordinary claim access. */
	private static boolean mayFightWithoutClaimAccess(Entity target) {
		// Leave PvP to the existing server and party rules.
		if (target instanceof Player) return true;
		if (!(target instanceof LivingEntity living) || !(target instanceof Enemy)) return false;
		if (NouveauSummons.owned(living) || target instanceof MenagerieAnimal animal && animal.owner() != null) return false;
		// Native vexes reference their evoker, not a player. Player-summoned vexes were excluded above.
		// Check the stored reference for other owned mobs, including an offline keeper.
		return !(target instanceof OwnableEntity ownable && ownable.getOwnerReference() != null && !(target instanceof Vex));
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
