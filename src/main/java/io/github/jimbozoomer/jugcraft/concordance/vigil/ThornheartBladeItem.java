package io.github.jimbozoomer.jugcraft.concordance.vigil;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Growth;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Thornheart Blade (roadmap step 16): living equipment, an iron-grade sword that develops through slaying, enduring
 * and being nourished ({@link Growth}; component {@code jugcraft:growth}). Each stage adds
 * {@value Vigil#STAGE_DAMAGE} attack damage while it has vigor, and every blow on a living creature spends
 * {@value Vigil#BLOW_VIGOR} vigor; without vigor its powers sleep (it fights as plain iron) but its stage is kept. The
 * bonus is the item's own attribute modifier, rebuilt whenever its stage or vigor changes, so vanilla applies it once
 * when the blade is taken in hand and removes it once when it is put away.
 */
public class ThornheartBladeItem extends Item {
	public static final Identifier GROWTH_MODIFIER = Jugcraft.id("thornheart_growth");

	public ThornheartBladeItem(Properties properties) {
		super(properties);
	}

	/** Rebuilds the blade's attack modifiers: the base sword's, plus its stage's bonus while it has vigor. */
	public static void refresh(ItemStack blade) {
		ItemAttributeModifiers base = blade.getItem().components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		Growth growth = Vigil.growth(blade);
		if (growth.stage() == 0 || growth.vigor() <= 0) {
			blade.set(DataComponents.ATTRIBUTE_MODIFIERS, base);
			return;
		}
		blade.set(DataComponents.ATTRIBUTE_MODIFIERS, base.withModifierAdded(Attributes.ATTACK_DAMAGE,
				new AttributeModifier(GROWTH_MODIFIER, growth.stage() * Vigil.STAGE_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND));
	}

	/** The bonus damage the blade gives now (0 while dormant or without vigor). */
	public static double bonus(ItemStack blade) {
		Growth growth = Vigil.growth(blade);
		return growth.vigor() > 0 ? growth.stage() * Vigil.STAGE_DAMAGE : 0.0;
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		if (attacker instanceof ServerPlayer && Vigil.enabled()) {
			Growth growth = Vigil.growth(stack);
			if (growth.stage() > 0 && growth.vigor() > 0) {
				Growth spent = growth.spend(Vigil.BLOW_VIGOR);
				if (spent != null) {
					stack.set(Vigil.GROWTH, spent);
					if (spent.vigor() == 0) {
						refresh(stack);
					}
				}
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		Growth growth = Vigil.growth(stack);
		tooltip.accept(Component.translatable("message.jugcraft.concordance.vigil.stage." + growth.stage()).withStyle(ChatFormatting.DARK_RED));
		tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.blade.deeds", growth.points(Growth.Deed.SLAY),
				growth.points(Growth.Deed.ENDURE), growth.points(Growth.Deed.NOURISH)).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable(growth.vigor() > 0 ? "tooltip.jugcraft.concordance.blade.vigor" : "tooltip.jugcraft.concordance.blade.asleep",
				growth.vigor(), Growth.MAX_VIGOR).withStyle(growth.vigor() > 0 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
		tooltip.accept(Component.translatable("tooltip.jugcraft.thornheart_blade").withStyle(ChatFormatting.GRAY));
	}
}
