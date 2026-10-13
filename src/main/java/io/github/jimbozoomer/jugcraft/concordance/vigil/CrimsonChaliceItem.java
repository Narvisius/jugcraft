package io.github.jimbozoomer.jugcraft.concordance.vigil;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Exhaustion;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Growth;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Offerings;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Rite;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Crimson Chalice (roadmap step 16): holds up to {@value Vigil#CHALICE_CAPACITY} Vitae (component
 * {@code jugcraft:vitae}). Used, it makes an offering (Crimson Rites understood); used while sneaking, a Crimson Surge;
 * used with a Thornheart Blade in the other hand, it feeds the blade. Each use says what happened, or exactly why not.
 */
public class CrimsonChaliceItem extends Item {
	public CrimsonChaliceItem(Properties properties) {
		super(properties);
	}

	public static int vitae(ItemStack stack) {
		Integer vitae = stack.get(Vigil.VITAE_HELD);
		return vitae == null ? 0 : Math.clamp(vitae, 0, Vigil.CHALICE_CAPACITY);
	}

	public static void setVitae(ItemStack stack, int vitae) {
		stack.set(Vigil.VITAE_HELD, Math.clamp(vitae, 0, Vigil.CHALICE_CAPACITY));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (!RateGate.allow(server, "vigil", 10)) {
			return InteractionResult.FAIL;
		}
		return use(server, player.getItemInHand(hand), player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND
				: InteractionHand.MAIN_HAND), player.isShiftKeyDown(), level.getGameTime());
	}

	/** One use at game time {@code now} (tests pass their own): feed a blade, surge, or offer. */
	public static InteractionResult use(ServerPlayer player, ItemStack chalice, ItemStack other, boolean sneaking, long now) {
		if (!Vigil.enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return InteractionResult.FAIL;
		}
		if (!Vigil.knows(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.vigil.unknown"));
			return InteractionResult.FAIL;
		}
		if (other.is(Vigil.THORNHEART_BLADE)) {
			Growth growth = Vigil.growth(other);
			if (growth.hunger() < Vigil.NOURISH_VITAE) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.vigil.sated"));
				return InteractionResult.FAIL;
			}
			if (Vigil.nourish(player, other, chalice, now) == 0) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.vigil.too_little", Vigil.NOURISH_VITAE));
				return InteractionResult.FAIL;
			}
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.vigil.fed", Vigil.growth(other).vigor(), Growth.MAX_VIGOR));
			return InteractionResult.SUCCESS;
		}
		if (sneaking) {
			String result = Vigil.surge(player, chalice, now);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.vigil." + result, Vigil.SURGE_VITAE, Vigil.SURGE_FOCUS));
			return result.equals("surged") ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		Offerings.Attempt attempt = Vigil.offer(player, chalice, now);
		Rite rite = Vigil.catalog().rite(Vigil.RITE);
		Exhaustion exhaustion = Vigil.offering(player).exhaustion();
		Component message = switch (attempt.outcome()) {
			case OFFERED -> Component.translatable("message.jugcraft.concordance.vigil.offered", attempt.health(), attempt.vitae(), vitae(chalice),
					Vigil.CHALICE_CAPACITY);
			case TOO_WEAK -> Component.translatable("message.jugcraft.concordance.vigil.too_weak", rite == null ? 0 : rite.floor());
			case EXHAUSTED -> Component.translatable("message.jugcraft.concordance.vigil.exhausted", (exhaustion.ticksToClear(now) + 19) / 20);
			case TOO_SOON -> Component.translatable("message.jugcraft.concordance.vigil.too_soon");
			case FULL -> Component.translatable("message.jugcraft.concordance.vigil.full");
		};
		player.sendOverlayMessage(message);
		return attempt.outcome() == Offerings.Outcome.OFFERED ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.chalice.vitae", vitae(stack), Vigil.CHALICE_CAPACITY)
				.withStyle(ChatFormatting.DARK_RED));
		tooltip.accept(Component.translatable("tooltip.jugcraft.crimson_chalice").withStyle(ChatFormatting.GRAY));
	}
}
