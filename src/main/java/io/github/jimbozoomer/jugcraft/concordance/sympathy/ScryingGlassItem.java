package io.github.jimbozoomer.jugcraft.concordance.sympathy;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.hex.Curse;
import io.github.jimbozoomer.jugcraft.concordance.hex.Hexes;
import io.github.jimbozoomer.jugcraft.concordance.hex.Link;
import io.github.jimbozoomer.jugcraft.concordance.hex.WardCategory;
import java.util.List;
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
import org.jspecify.annotations.Nullable;

/**
 * A scrying glass (roadmap step 22): the investigation and the remedy. Used with nothing in the other hand it looks at
 * the curses on its user ({@value Hexes#INVESTIGATE_FOCUS} Focus a look): first their names and remedies, then their
 * casters, unless a caster's scrying ward hides them. With a remedy in the other hand it lifts the curses that remedy
 * answers, taking one. With a taglock in the other hand it reads the link: its strength and whether its target can be
 * found now (a target's scrying ward hides where they are). Anyone may use one: countermeasures need no research.
 */
public class ScryingGlassItem extends Item {
	public ScryingGlassItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel) {
			if (!RateGate.allow(server, "hex", 10)) {
				return InteractionResult.FAIL;
			}
			look(server, serverLevel, player.getOffhandItem());
		}
		return InteractionResult.SUCCESS;
	}

	/** One use of the glass with {@code other} in the other hand (tests call this). */
	public static void look(ServerPlayer player, ServerLevel level, ItemStack other) {
		if (!Sympathy.enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return;
		}
		long now = level.getGameTime();
		if (other.is(Sympathy.TAGLOCK)) {
			readLink(player, level, other.get(Sympathy.LINK), now);
			return;
		}
		if (!other.isEmpty()) {
			int lifted = Sympathy.remedy(player, BuiltInRegistries.ITEM.getKey(other.getItem()).toString());
			if (lifted > 0) {
				other.shrink(1);
				player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.hex.remedied", lifted));
			} else {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.hex.no_remedy"));
			}
			return;
		}
		if (Sympathy.curses(player).stream().noneMatch(curse -> curse.active(now))) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.hex.clean"));
			return;
		}
		if (!ConcordanceProgress.spendFocus(player, Hexes.INVESTIGATE_FOCUS)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.hex.refused",
					Component.translatable("compose.jugcraft.hex.reason.no_focus")));
			return;
		}
		List<Curse> known = Sympathy.investigate(player, level);
		for (Curse curse : known) {
			player.sendSystemMessage(Sympathy.describe(curse, now));
		}
	}

	private static void readLink(ServerPlayer player, ServerLevel level, @Nullable Link link, long now) {
		if (link == null) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.hex.link_none"));
			return;
		}
		Component where;
		boolean sameDimension = link.dimension().equals(level.dimension().identifier().toString());
		LivingEntity target = sameDimension ? Sympathy.find(level, link) : null;
		// Roadmap step 28: scrying finds no more than a curse could reach: the link's maker, within range, by the
		// multiplayer rules.
		if (!link.linker().equals(player.getUUID())) {
			where = Component.translatable("compose.jugcraft.hex.reason.not_yours");
		} else if (link.expired(now)) {
			where = Component.translatable("compose.jugcraft.hex.reason.expired");
		} else if (!sameDimension) {
			where = Component.translatable("compose.jugcraft.hex.reason.elsewhere");
		} else if (target == null) {
			where = Component.translatable("compose.jugcraft.hex.reason.not_found");
		} else if (player.distanceToSqr(target) > (double) Hexes.MAX_RANGE * Hexes.MAX_RANGE) {
			where = Component.translatable("compose.jugcraft.hex.reason.too_far");
		} else if (!Sympathy.allowed(player, target)) {
			where = Component.translatable("compose.jugcraft.hex.reason.not_allowed");
		} else if (Sympathy.warded(target, WardCategory.SCRYING)) {
			where = Component.translatable("compose.jugcraft.hex.reason.hidden");
		} else {
			where = Component.translatable("message.jugcraft.concordance.hex.link_distance", (int) Math.sqrt(player.distanceToSqr(target)));
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.hex.link_status", link.strength(now), Link.FRESH, where));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.scrying_glass").withStyle(ChatFormatting.GRAY));
	}
}
