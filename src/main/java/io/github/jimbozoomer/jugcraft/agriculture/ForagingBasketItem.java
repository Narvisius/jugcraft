package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Foraging Basket: a wicker basket that holds what a forager finds, like a bundle, and only that (item tag
 * {@code jugcraft:forage}: wild mushrooms, berries, nuts and wild fruit). Wild mushrooms picked by hand with a basket in
 * either hand go straight into it. A basket holding all five wild mushrooms earns Forager.
 */
public class ForagingBasketItem extends BundleItem {
	public static final TagKey<Item> FORAGE = TagKey.create(Registries.ITEM, Jugcraft.id("forage"));

	public ForagingBasketItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack basket, Slot slot, ClickAction action, Player player) {
		ItemStack other = slot.getItem();
		return (other.isEmpty() || other.is(FORAGE)) && super.overrideStackedOnOther(basket, slot, action, player);
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack basket, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
		return (other.isEmpty() || other.is(FORAGE)) && super.overrideOtherStackedOnMe(basket, other, slot, action, player, access);
	}

	/** A Foraging Basket in either of {@code player}'s hands, or empty. */
	public static ItemStack held(Player player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof ForagingBasketItem) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	/** Puts as much of {@code found} as fits into the basket (forage only); {@code found} keeps what did not fit. */
	public static void fill(ItemStack basket, ItemStack found, Player player) {
		BundleContents contents = basket.get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null || found.isEmpty() || !found.is(FORAGE)) {
			return;
		}
		BundleContents.Mutable mutable = contents.asMutable();
		int added = mutable.tryInsert(found.copy());
		if (added > 0) {
			found.shrink(added);
			basket.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
			if (player instanceof ServerPlayer server && holdsEveryMushroom(basket)) {
				TrickOrTreat.award(server, "forager");
			}
		}
	}

	/** Whether {@code basket} holds each of the five wild mushrooms. */
	public static boolean holdsEveryMushroom(ItemStack basket) {
		BundleContents contents = basket.get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null) {
			return false;
		}
		for (String mushroom : JugcraftAgriculture.WILD_MUSHROOMS) {
			Item item = JugcraftAgriculture.item(mushroom);
			if (contents.itemCopies().noneMatch(stack -> stack.is(item))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("item.jugcraft.foraging_basket.hint").withStyle(ChatFormatting.GRAY));
	}
}
