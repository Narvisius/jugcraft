package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Bonding Charm (roadmap step 17): binds a Hearthling familiar to its user (Binding Arts understood), one familiar
 * a person; used again it recalls the familiar to their side (only from the same dimension, and only if it is loaded:
 * nothing is loaded to fetch it); used while sneaking it releases the familiar.
 */
public class BondingCharmItem extends Item {
	public BondingCharmItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (!RateGate.allow(server, "workers", 10)) {
			return InteractionResult.FAIL;
		}
		return use(server, serverLevel, player.isShiftKeyDown());
	}

	/** Bind, recall or (sneaking) release; tests call this directly. */
	public static InteractionResult use(ServerPlayer player, ServerLevel level, boolean sneaking) {
		if (!Workers.enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return InteractionResult.FAIL;
		}
		if (!Workers.knows(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.unknown"));
			return InteractionResult.FAIL;
		}
		WorkerRoster roster = WorkerRoster.of(level.getServer());
		UUID familiar = roster.familiar(player.getUUID());
		Entity found = familiar == null ? null : level.getEntity(familiar);
		if (familiar == null) {
			if (sneaking) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.no_familiar"));
				return InteractionResult.FAIL;
			}
			HearthlingEntity bound = Workers.HEARTHLING.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (bound == null) {
				return InteractionResult.FAIL;
			}
			bound.setOwner(player.getUUID());
			bound.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
			level.addFreshEntity(bound);
			roster.note(player.getUUID(), bound.getUUID(), bound.kind(), bound.status(), level.dimension().identifier().toString(), bound.blockPosition());
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.bound"));
			return InteractionResult.SUCCESS;
		}
		if (sneaking) {
			if (found instanceof HearthlingEntity hearthling) {
				hearthling.discard();
			}
			roster.remove(player.getUUID(), familiar);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.released"));
			return InteractionResult.SUCCESS;
		}
		if (!(found instanceof HearthlingEntity hearthling)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.not_here"));
			return InteractionResult.FAIL;
		}
		hearthling.teleportTo(player.getX(), player.getY(), player.getZ());
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.recalled"));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.bonding_charm").withStyle(ChatFormatting.GRAY));
	}
}
