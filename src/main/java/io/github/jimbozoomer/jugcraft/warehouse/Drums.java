package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.fluid.StoredFluid;
import io.github.jimbozoomer.jugcraft.nuclear.TankProducts;
import java.util.Locale;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Part W4 (claude/jugcraft-warehouse-plan.md, "W4 design"): liquids stored in drums on pallets. A drum holds
 * {@link #CAPACITY} mB of one liquid, four to a pallet; the liquid picks the drum by its nuclear/TankProducts kind (blue
 * steel for a general liquid, red steel for a flammable one, white HDPE for a corrosive one, a stainless dewar for a
 * cryogenic one; gases and lava are turned away).
 *
 * <p>In the warehouse's books a drum is a case (CaseLoad) whose item is the drum's item carrying the liquid
 * ({@link #LIQUID}: what the stock is counted by) and whose count is the liquid in it, in millibuckets. An emptied
 * drum stays on its pallet as a case of 0 until the pallet goes back to a Drum Filling Station. A drum dropped in the
 * world (a pallet lost, a station broken) is the drum's item with the liquid in it ({@code jugcraft:stored_fluid}).
 */
public final class Drums {
	/** What a drum holds (mB), and how many go on a pallet. */
	public static final long CAPACITY = 20_000;
	public static final int PER_PALLET = 4;
	/** The rack's LOAD for a pallet of drums: after the case loads (tools/warehouse_data.py DRUM_LOAD0). */
	public static final int LOAD0 = 1 + 7 * PalletLoad.MAX_CASES;

	public enum Kind {
		STEEL("steel_drum", "steel"), RED("flammables_drum", "red"), POLY("hdpe_drum", "poly"), DEWAR("liquid_dewar", "dewar");

		public final String item, model;

		Kind(String item, String model) {
			this.item = item;
			this.model = model;
		}

		/** The hidden part the game draws one of these drums with. */
		public String part() {
			return "drum_" + model;
		}
	}

	/** The liquid a drum in the books holds (the stock's key: the drum's item with this on it). */
	public static DataComponentType<FluidVariant> LIQUID;
	public static final Item[] ITEMS = new Item[Kind.values().length];

	private Drums() {
	}

	static void registerContent() {
		LIQUID = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("drum_liquid"),
				DataComponentType.<FluidVariant>builder().persistent(FluidVariant.CODEC).networkSynchronized(FluidVariant.PACKET_CODEC).build());
		for (Kind k : Kind.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(k.item));
			Item item = Registry.register(BuiltInRegistries.ITEM, key, new DrumItem(k, new Item.Properties().setId(key).stacksTo(16)));
			ITEMS[k.ordinal()] = item;
			JugcraftWarehouse.ITEMS.add(item);
		}
	}

	static void registerHooks() {
		FluidStorage.ITEM.registerForItems((stack, context) -> DrumItem.storage(context), ITEMS);
	}

	/** The drum a liquid goes in, or null when the warehouse turns it away (gases, lava). */
	public static @Nullable Kind kindFor(FluidVariant liquid) {
		if (liquid == null || liquid.isBlank()) {
			return null;
		}
		return switch (TankProducts.kind(liquid)) {
			case GENERAL -> Kind.STEEL;
			case FLAMMABLE -> Kind.RED;
			case CORROSIVE -> Kind.POLY;
			case CRYOGENIC -> Kind.DEWAR;
			case REFUSED -> null;
		};
	}

	public static Item item(Kind kind) {
		return ITEMS[kind.ordinal()];
	}

	public static @Nullable Kind kindOf(ItemStack stack) {
		return stack.getItem() instanceof DrumItem drum ? drum.kind : null;
	}

	/** A drum of a liquid as the books count it (the stock's key; a case's item). */
	public static ItemStack book(FluidVariant liquid) {
		Kind kind = kindFor(liquid);
		ItemStack out = new ItemStack(item(kind == null ? Kind.STEEL : kind));
		out.set(LIQUID, liquid);
		return out;
	}

	/** The liquid a book drum stands for, or null (not a liquid). */
	public static @Nullable FluidVariant liquid(ItemStack stack) {
		if (LIQUID == null || stack.isEmpty()) {
			return null;
		}
		FluidVariant v = stack.get(LIQUID);
		return v == null || v.isBlank() ? null : v;
	}

	public static boolean isLiquid(ItemStack stack) {
		return liquid(stack) != null;
	}

	public static boolean isLiquid(CaseLoad c) {
		return isLiquid(c.item());
	}

	/** Whether a pallet holds drums of a liquid (it holds nothing else then). */
	public static boolean isLiquid(@Nullable PalletLoad load) {
		return load != null && !load.isEmpty() && isLiquid(load.cases().get(0));
	}

	/** A drum as it drops in the world: its item, the liquid in it (if any) kept on it. */
	public static ItemStack dropped(CaseLoad c) {
		Kind kind = kindOf(c.item());
		ItemStack out = new ItemStack(item(kind == null ? Kind.STEEL : kind));
		FluidVariant liquid = liquid(c.item());
		if (liquid != null && c.count() > 0) {
			out.set(JugcraftFluids.STORED_FLUID, new StoredFluid(liquid, c.count() * FluidNetworks.DROPLETS_PER_MB));
		}
		return out;
	}

	/** A liquid's name ("Diesel"). */
	public static Component name(FluidVariant liquid) {
		return FluidVariantAttributes.getName(liquid);
	}

	/** A stock line's or an order's name: the liquid's for a book drum, else the item's. */
	public static Component name(ItemStack stack) {
		FluidVariant liquid = liquid(stack);
		return liquid != null ? name(liquid) : stack.getHoverName();
	}

	/** Millibuckets as buckets: "80 B", "14.5 B", "1,216 B". */
	public static String buckets(long mb) {
		if (mb % 1000 == 0 || mb >= 100_000) {
			return String.format(Locale.ROOT, "%,d B", Math.round(mb / 1000.0));
		}
		return String.format(Locale.ROOT, "%,.1f B", mb / 1000.0);
	}

	/** Droplets as millibuckets (whole). */
	public static long mb(long droplets) {
		return droplets / FluidNetworks.DROPLETS_PER_MB;
	}

	public static long droplets(long mb) {
		return mb * FluidNetworks.DROPLETS_PER_MB;
	}
}
