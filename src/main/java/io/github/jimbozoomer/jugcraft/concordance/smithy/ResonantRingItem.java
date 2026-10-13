package io.github.jimbozoomer.jugcraft.concordance.smithy;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Artifice;
import io.github.jimbozoomer.jugcraft.concordance.artifice.RolledAffix;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Stat;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Resonant Ring (roadmap step 19): a Trinkets Updated ring (the hand ring slot). Its properties live in its
 * {@code jugcraft:artifice} component; Trinkets asks it for its modifiers when it is equipped and removes them when it is
 * taken off, so each applies exactly once. It gives nothing when dull or bonded to someone else, and nothing while held
 * (it has no hand modifiers). Worn, it wears one point every {@value Artificery#WEAR_TICKS} ticks, down to its last
 * point, never further: a dull ring is repaired, never broken.
 */
public class ResonantRingItem extends Item implements TrinketCallback {
	public ResonantRingItem(Properties properties) {
		super(properties);
	}

	@Override
	public void forEachTrinketModifier(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, Identifier slotIdentifier,
			BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		List<Stat> stats = Artificery.stats(stack, entity.getUUID());
		for (int i = 0; i < stats.size(); i++) {
			Stat stat = stats.get(i);
			Identifier id = Identifier.tryParse(stat.attribute());
			if (id == null) {
				continue;
			}
			Identifier modifier = slotIdentifier.withSuffix("/jugcraft_artifice_" + i);
			BuiltInRegistries.ATTRIBUTE.get(id).ifPresent(attribute -> consumer.accept(attribute,
					new AttributeModifier(modifier, stat.amount(), Artificery.operation(stat.operation()))));
		}
	}

	@Override
	public void tick(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
		if (entity.level().isClientSide() || entity.tickCount % Artificery.WEAR_TICKS != 0 || stack.getMaxDamage() <= 0
				|| Artificery.dull(stack)) {
			return;
		}
		stack.setDamageValue(stack.getDamageValue() + 1);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		Artifice artifice = stack.get(Artificery.ARTIFICE);
		if (artifice == null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.resonant_ring").withStyle(ChatFormatting.GRAY));
			return;
		}
		// Only what the ring itself carries: the rules (costs, capacity) are the server's and are not sent to clients.
		tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.artifice.ring",
				Component.translatable("compose.jugcraft.artifice.quality." + artifice.quality().id),
				Component.translatable("compose.jugcraft.artifice.substrate." + Artificery.path(artifice.substrate()))).withStyle(ChatFormatting.GOLD));
		for (RolledAffix rolled : artifice.affixes()) {
			tooltip.accept(Component.translatable("message.jugcraft.concordance.artifice.affix",
					Component.translatable("compose.jugcraft.artifice.affix." + Artificery.path(rolled.affix())), "+" + Artificery.format(rolled.value()))
					.withStyle(ChatFormatting.GRAY));
		}
		for (String rune : artifice.runes()) {
			tooltip.accept(Component.translatable("message.jugcraft.concordance.artifice.rune",
					Component.translatable("compose.jugcraft.artifice.rune." + Artificery.path(rune))).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		for (String gem : artifice.gems()) {
			tooltip.accept(Component.translatable("message.jugcraft.concordance.artifice.gem",
					Component.translatable("compose.jugcraft.artifice.gem." + Artificery.path(gem))).withStyle(ChatFormatting.AQUA));
		}
		if (artifice.bond() != null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.artifice.bonded").withStyle(ChatFormatting.GRAY));
		}
		if (Artificery.dull(stack)) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.artifice.dull").withStyle(ChatFormatting.RED));
		}
	}
}
