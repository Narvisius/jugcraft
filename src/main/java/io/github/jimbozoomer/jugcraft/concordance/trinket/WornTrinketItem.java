package io.github.jimbozoomer.jugcraft.concordance.trinket;

import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.Reliquary;
import java.util.Map;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A belt, charm or boot of {@link Wayfaring}, worn in a Trinkets slot given by data. Its attributes are Trinkets
 * modifiers, applied while it is worn where Trinkets applies effects and removed when it comes off, as the Ember foci's
 * are; in the hand or a container it gives nothing. Each modifier is named after the item's kind and the attribute, not
 * the slot, so a second of a kind adds nothing (Trinkets keeps one modifier per id, and keeps it while any copy is
 * worn), and the Phoenix Down, of the feather's kind, gives exactly the feather's.
 * <p>
 * Only someone who understands Relic Lore can put one on ({@link #canEquip}): asked on the server and, for one's own
 * screen, on the client, which holds its own player's research. A Leather Belt cannot be taken off while the Charm slot
 * it adds holds something (Trinkets would drop that charm on the ground).
 */
public class WornTrinketItem extends Item implements TrinketCallback {
	private final String kind;
	private final Map<String, Double> modifiers;

	/**
	 * @param kind the kind its modifiers are named after (its own id, or the feather's for the Phoenix Down)
	 * @param modifiers attribute id to the value it adds while worn
	 */
	public WornTrinketItem(Properties properties, String kind, Map<String, Double> modifiers) {
		super(properties);
		this.kind = kind;
		this.modifiers = Map.copyOf(modifiers);
	}

	/** What it adds worn: attribute id to value. */
	public Map<String, Double> modifiers() {
		return modifiers;
	}

	/** The id of the modifier it gives {@code attribute} (the same from any slot, and for any item of its kind). */
	public Identifier modifierId(Identifier attribute) {
		return Jugcraft.id("wayfaring/" + kind + "/" + attribute.getPath());
	}

	@Override
	public void forEachTrinketModifier(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, Identifier slotIdentifier,
			BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		modifiers.forEach((attribute, value) -> {
			Identifier id = Identifier.parse(attribute);
			BuiltInRegistries.ATTRIBUTE.get(id).ifPresent(holder -> consumer.accept(holder,
					new AttributeModifier(modifierId(id), value, AttributeModifier.Operation.ADD_VALUE)));
		});
	}

	@Override
	public boolean canEquip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
		return entity instanceof Player player && Reliquary.knows(player);
	}

	@Override
	public boolean canUnequip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
		// Only the belt actually worn holds the slot: Trinkets asks about a cosmetic stack with the worn slot's access.
		if (!slot.cosmetic() && slot.get() == stack && stack.is(Wayfaring.LEATHER_BELT) && holdsAddedCharm(entity)) {
			return false;
		}
		return TrinketCallback.super.canUnequip(stack, slot, entity);
	}

	/**
	 * Worn for show (a cosmetic slot, where a server enables them), it stays with its wearer through death: Trinkets drops
	 * a cosmetic stack on death but leaves it in its slot, so the respawned player would have it too.
	 */
	@Override
	public TrinketDropRule getDropRule(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
		return slot.cosmetic() ? TrinketDropRule.KEEP : TrinketCallback.super.getDropRule(stack, slot, entity);
	}

	/** Whether a Charm slot beyond those a player has without a belt holds something. */
	public static boolean holdsAddedCharm(LivingEntity entity) {
		var charms = TrinketsApi.getAttachment(entity).getInventory(Wayfaring.CHARM_SLOT);
		if (charms == null) {
			return false;
		}
		for (int i = Wayfaring.CHARM_SLOTS; i < charms.getContainerSize(); i++) {
			if (!charms.getItem(i).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		Identifier id = BuiltInRegistries.ITEM.getKey(this);
		tooltip.accept(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY));
	}
}
