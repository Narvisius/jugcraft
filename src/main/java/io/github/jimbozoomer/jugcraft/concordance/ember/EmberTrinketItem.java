package io.github.jimbozoomer.jugcraft.concordance.ember;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
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
 * A worn piece of the regalia ({@link EmberGear}): the foci in the Spell Focus slot, the bangle in a Bracelet slot
 * (Trinkets Updated slots given by data). A focus's fire Spell Power is a Trinkets modifier, applied when it is worn and
 * removed when it is taken off, as the Resonant Ring's are; it depends on nothing but the item, so a focus in the hand,
 * or in a slot that applies no effects, gives nothing. The bangle gives no Spell Power: its blow is EmberGear's.
 */
public class EmberTrinketItem extends Item implements TrinketCallback {
	private final double fire;

	public EmberTrinketItem(Properties properties, double fire) {
		super(properties);
		this.fire = fire;
	}

	/** The fire Spell Power it gives worn, in points above the school's base. */
	public double fire() {
		return fire;
	}

	@Override
	public void forEachTrinketModifier(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, Identifier slotIdentifier,
			BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		if (fire <= 0.0) {
			return;
		}
		Identifier modifier = slotIdentifier.withSuffix("/jugcraft_ember");
		BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(Ember.SCHOOL)).ifPresent(attribute -> consumer.accept(attribute,
				new AttributeModifier(modifier, fire, AttributeModifier.Operation.ADD_VALUE)));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		Identifier id = BuiltInRegistries.ITEM.getKey(this);
		tooltip.accept(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY));
	}
}
