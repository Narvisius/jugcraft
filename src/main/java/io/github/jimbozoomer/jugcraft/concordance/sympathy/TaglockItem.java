package io.github.jimbozoomer.jugcraft.concordance.sympathy;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.hex.CurseDefinition;
import io.github.jimbozoomer.jugcraft.concordance.hex.Hexes;
import io.github.jimbozoomer.jugcraft.concordance.hex.Link;
import io.github.jimbozoomer.jugcraft.concordance.hex.WardCategory;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A taglock (roadmap step 22). Touched to a creature (used on it) by someone who understands Sympathy, it takes a link
 * to it: never to oneself, never to a player the multiplayer rules forbid harming, never through a linking ward. Held
 * with a curse's reagent in the other hand and used, it casts that curse through the link, once {@link Sympathy#cast}
 * allows. A link fades with time and is revalidated at every use.
 */
public class TaglockItem extends Item {
	public TaglockItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server) || !(player.level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		if (!RateGate.allow(server, "hex", 6)) {
			return InteractionResult.FAIL;
		}
		bind(server, level, player.getItemInHand(hand), target);
		return InteractionResult.SUCCESS;
	}

	/** Takes a link to {@code target} into {@code taglock} (tests call this). Returns the reason it cannot, or "". */
	public static String bind(ServerPlayer player, ServerLevel level, ItemStack taglock, LivingEntity target) {
		String reason;
		if (!Sympathy.enabled()) {
			reason = "disabled";
		} else if (!Sympathy.knows(player)) {
			reason = "unknown";
		} else {
			reason = Hexes.link(player.getUUID(), target.getUUID(), Sympathy.warded(target, WardCategory.LINKING), Sympathy.allowed(player, target));
		}
		if (!reason.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.hex.refused",
					Component.translatable("compose.jugcraft.hex.reason." + reason)));
			return reason;
		}
		taglock.set(Sympathy.LINK, new Link(player.getUUID(), target.getUUID(), target instanceof Player ? "player" : "creature",
				level.dimension().identifier().toString(), level.getGameTime(), Link.FRESH));
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.hex.linked", target.getDisplayName()));
		return "";
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel) {
			if (!RateGate.allow(server, "hex", 6)) {
				return InteractionResult.FAIL;
			}
			curse(server, serverLevel, player.getMainHandItem(), player.getOffhandItem());
		}
		return InteractionResult.SUCCESS;
	}

	/** Casts the curse whose reagent {@code reagent} is through the link {@code taglock} holds (tests call this). */
	public static String curse(ServerPlayer player, ServerLevel level, ItemStack taglock, ItemStack reagent) {
		Link link = taglock.is(Sympathy.TAGLOCK) ? taglock.get(Sympathy.LINK) : null;
		CurseDefinition curse = reagent.isEmpty() ? null : Sympathy.catalog().byReagent(BuiltInRegistries.ITEM.getKey(reagent.getItem()).toString());
		String reason;
		if (!Sympathy.enabled()) {
			reason = "disabled";
		} else if (!Sympathy.knows(player)) {
			reason = "unknown";
		} else if (curse == null) {
			reason = "no_reagent";
		} else {
			reason = Sympathy.cast(player, level, link, curse);
		}
		if (!reason.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.hex.refused",
					Component.translatable("compose.jugcraft.hex.reason." + reason)));
			return reason;
		}
		ConcordanceProgress.spendFocus(player, curse.focus());
		reagent.shrink(1);
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.hex.cast", Sympathy.curseName(curse.id())));
		return "";
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		Link link = stack.get(Sympathy.LINK);
		tooltip.accept(Component.translatable(link == null ? "tooltip.jugcraft.taglock" : "tooltip.jugcraft.concordance.hex.taglock_bound",
				link == null ? "" : Component.translatable("compose.jugcraft.hex.kind." + link.kind())).withStyle(ChatFormatting.GRAY));
	}
}
