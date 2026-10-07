package io.github.jimbozoomer.jugcraft.machine;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jspecify.annotations.Nullable;

/**
 * Electroplating (batch 34, docs/features/electroplating.md): what the electroplating bath does to a tool, weapon or
 * piece of armor. A plating is stored on the item ({@link #PLATING}: "nickel", "silver" or "gold") and every plating
 * repairs the item fully. Plating again with the same metal only repairs; a different metal is refused. Keep the
 * numbers in sync with tools/electroplating.py.
 * <ul>
 * <li>Nickel: the item's durability is {@link #NICKEL_DURABILITY_PERCENT} percent of what it was.</li>
 * <li>Silver: a sword or axe gets Smite {@link #SILVER_SMITE} (raised to it if lower).</li>
 * <li>Gold: armor counts as gold for piglins (the mixin {@code PiglinSafeArmorMixin} asks {@link #wearsGold}).</li>
 * </ul>
 */
public final class Electroplating {
	/** Ticks per plating, sulfuric acid each one uses (mB), and the bath's acid tank (mB). */
	public static final int TICKS = 200;
	public static final int ACID_PER_PLATING = 100;
	public static final int TANK = 4_000;
	public static final int NICKEL_DURABILITY_PERCENT = 150;
	public static final int SILVER_SMITE = 3;

	public static final TagKey<Item> NICKEL = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/nickel"));
	public static final TagKey<Item> SILVER = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/silver"));
	public static final TagKey<Item> GOLD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/gold"));

	/** The metal an item is plated with. */
	public static DataComponentType<String> PLATING;

	private Electroplating() {
	}

	public static void register() {
		PLATING = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("plating"),
				DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
	}

	/** The metal an ingot plates with, or null if it is not a plating metal. */
	public static @Nullable String metalOf(ItemStack ingot) {
		if (ingot.is(NICKEL)) {
			return "nickel";
		}
		if (ingot.is(SILVER)) {
			return "silver";
		}
		return ingot.is(GOLD) || ingot.is(Items.GOLD_INGOT) ? "gold" : null;
	}

	/** Whether an item can go in the bath at all: anything that wears out. */
	public static boolean platable(ItemStack stack) {
		return stack.isDamageableItem();
	}

	/**
	 * {@code item} plated with {@code metal}, repaired; or an empty stack if it cannot be (it does not wear out, or it is
	 * already plated with another metal). {@code item} itself is not changed.
	 */
	public static ItemStack plate(ItemStack item, String metal, HolderLookup.Provider registries) {
		if (!platable(item)) {
			return ItemStack.EMPTY;
		}
		String existing = item.get(PLATING);
		if (existing != null && !existing.equals(metal)) {
			return ItemStack.EMPTY;
		}
		ItemStack out = item.copyWithCount(1);
		if (existing == null) {
			out.set(PLATING, metal);
			if (metal.equals("nickel")) {
				out.set(DataComponents.MAX_DAMAGE, out.getMaxDamage() * NICKEL_DURABILITY_PERCENT / 100);
			} else if (metal.equals("silver") && (out.is(ItemTags.SWORDS) || out.is(ItemTags.AXES))) {
				var smite = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SMITE);
				EnchantmentHelper.updateEnchantments(out, enchantments -> enchantments.upgrade(smite, SILVER_SMITE));
			}
		}
		out.setDamageValue(0);
		return out;
	}

	/** Whether {@code entity} wears a gold-plated piece of armor, which piglins take for gold. */
	public static boolean wearsGold(LivingEntity entity) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && "gold".equals(entity.getItemBySlot(slot).get(PLATING))) {
				return true;
			}
		}
		return false;
	}
}
