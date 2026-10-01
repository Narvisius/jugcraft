package io.github.jimbozoomer.jugcraft.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Upgrade modules for the powered tools, fitted at the charging station (use a module on a station
 * holding the tool). Fitting uses the module up; modules cannot be taken out again.
 * <ul>
 * <li>Overclock: each one adds half the tool's speed again and adds its base JE per block again (max 2).</li>
 * <li>Range: the drill's area mode mines 5×5 instead of 3×3 (drill only, max 1).</li>
 * <li>Capacity: each one adds the item's base capacity again (max 2; tools and the rocket pack).</li>
 * <li>Silk touch and fortune: the vanilla enchantments (silk touch on the drill or chainsaw; fortune up to III on
 * the drill); a tool takes one or the other.</li>
 * </ul>
 */
public final class ToolUpgrades {
	public enum Kind {
		OVERCLOCK("overclock", 2, stack -> stack.getItem() instanceof PoweredToolItem, null),
		RANGE("range", 1, stack -> stack.getItem() instanceof MiningDrillItem, null),
		CAPACITY("capacity", 2, stack -> stack.getItem() instanceof Chargeable, null),
		SILK_TOUCH("silk_touch", 1, stack -> stack.getItem() instanceof PoweredToolItem, Enchantments.SILK_TOUCH),
		FORTUNE("fortune", 3, stack -> stack.getItem() instanceof MiningDrillItem, Enchantments.FORTUNE);

		public final String id;
		public final int max;
		final Predicate<ItemStack> fits;
		/** For silk touch and fortune: the enchantment the module adds; the others count in a component. */
		final @Nullable ResourceKey<Enchantment> enchantment;

		Kind(String id, int max, Predicate<ItemStack> fits, @Nullable ResourceKey<Enchantment> enchantment) {
			this.id = id;
			this.max = max;
			this.fits = fits;
			this.enchantment = enchantment;
		}

		@Nullable DataComponentType<Integer> component() {
			return switch (this) {
				case OVERCLOCK -> JugcraftTools.OVERCLOCK;
				case RANGE -> JugcraftTools.RANGE;
				case CAPACITY -> JugcraftTools.CAPACITY;
				default -> null;
			};
		}
	}

	/** Why a module was not fitted, as a message key suffix, or {@code "fitted"}. */
	public enum Result {
		FITTED, WRONG_TOOL, FULL, CONFLICT
	}

	private ToolUpgrades() {
	}

	/** Modules of this kind in the stack (for silk touch and fortune, the enchantment level). */
	public static int level(ItemStack stack, Kind kind) {
		DataComponentType<Integer> component = kind.component();
		return component == null ? 0 : stack.getOrDefault(component, 0);
	}

	private static int enchantmentLevel(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
		return stack.getEnchantments().getLevel(enchantment(level, key));
	}

	private static Holder<Enchantment> enchantment(Level level, ResourceKey<Enchantment> key) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	/** Fits one module of this kind into the tool, if it takes one. */
	public static Result fit(Level level, ItemStack tool, Kind kind) {
		if (!kind.fits.test(tool)) {
			return Result.WRONG_TOOL;
		}
		if (kind.enchantment != null) {
			ResourceKey<Enchantment> other = kind == Kind.SILK_TOUCH ? Enchantments.FORTUNE : Enchantments.SILK_TOUCH;
			if (enchantmentLevel(level, tool, other) > 0) {
				return Result.CONFLICT;
			}
			int current = enchantmentLevel(level, tool, kind.enchantment);
			if (current >= kind.max) {
				return Result.FULL;
			}
			tool.enchant(enchantment(level, kind.enchantment), current + 1);
			return Result.FITTED;
		}
		int current = level(tool, kind);
		if (current >= kind.max) {
			return Result.FULL;
		}
		tool.set(kind.component(), current + 1);
		return Result.FITTED;
	}

	/** "Upgrades: Overclock ×2, Range" (silk touch and fortune show as the enchantments they are). */
	static void appendTooltip(ItemStack stack, Consumer<Component> tooltip) {
		List<Component> parts = new ArrayList<>();
		for (Kind kind : Kind.values()) {
			int count = level(stack, kind);
			if (count > 0) {
				MutableComponent name = Component.translatable("item.jugcraft." + kind.id + "_module.short");
				parts.add(count > 1 ? name.append(" ×" + count) : name);
			}
		}
		if (!parts.isEmpty()) {
			MutableComponent line = Component.translatable("tooltip.jugcraft.upgrades");
			for (int i = 0; i < parts.size(); i++) {
				line.append(i == 0 ? " " : ", ").append(parts.get(i));
			}
			tooltip.accept(line.withStyle(ChatFormatting.DARK_AQUA));
		}
	}
}
