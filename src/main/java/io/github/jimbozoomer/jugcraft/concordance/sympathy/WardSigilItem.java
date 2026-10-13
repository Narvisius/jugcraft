package io.github.jimbozoomer.jugcraft.concordance.sympathy;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.hex.Ward;
import io.github.jimbozoomer.jugcraft.concordance.hex.WardCategory;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
 * A ward sigil (roadmap step 22): used, it wards its user against its one category of operation (its
 * {@link Sympathy#WARD} component: linking, cursing, scrying or moving) for twenty minutes, and is spent. A fresh sigil of
 * a category already held renews it; wards never stack. Anyone may use one.
 */
public class WardSigilItem extends Item {
	public WardSigilItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack sigil = player.getItemInHand(hand);
		if (player instanceof ServerPlayer server) {
			if (!RateGate.allow(server, "ward", 10)) {
				return InteractionResult.FAIL;
			}
			apply(server, sigil);
		}
		return InteractionResult.SUCCESS;
	}

	/** Wards {@code player} by {@code sigil}'s category and spends it; returns whether it did (tests call this). */
	public static boolean apply(ServerPlayer player, ItemStack sigil) {
		WardCategory category = WardCategory.fromId(sigil.getOrDefault(Sympathy.WARD, ""));
		if (category == null || !Sympathy.enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.hex.blank_sigil"));
			return false;
		}
		Sympathy.ward(player, category);
		sigil.shrink(1);
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.hex.warded",
				Component.translatable("compose.jugcraft.hex.ward." + category.id), Ward.TICKS / 20 / 60));
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		String category = stack.getOrDefault(Sympathy.WARD, "");
		tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.hex.sigil",
				Component.translatable("compose.jugcraft.hex.ward." + (WardCategory.fromId(category) == null ? "none" : category)))
				.withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("tooltip.jugcraft.ward_sigil").withStyle(ChatFormatting.GRAY));
	}
}
