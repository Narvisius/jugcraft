package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

/**
 * The petroleum fluids of the Chemistry branch's oil line (docs/branches/CHEMISTRY.md). Each is a real fluid with a
 * source and a flowing form, a liquid block and a bucket, so it works with Jugcraft's pumps, pipes and tanks and
 * with any other mod's through Fabric's fluid API. Names and flow come from FLUIDS in tools/petro.py.
 */
public final class PetroFluids {
	/** One fluid. Filled in by {@link #register}; the fluid classes read it once registration is done. */
	public static final class Entry {
		private final String id;
		private final int tickDelay;
		private final int slope;
		private final int dropOff;
		private final int color;
		private FlowingFluid source;
		private FlowingFluid flowing;
		private LiquidBlock block;
		private Item bucket;

		Entry(String id, int tickDelay, int slope, int dropOff, int color) {
			this.id = id;
			this.color = color;
			this.tickDelay = tickDelay;
			this.slope = slope;
			this.dropOff = dropOff;
		}

		public String id() {
			return id;
		}

		/** ARGB colour for tank gauges (the middle shade of its texture). */
		public int color() {
			return color;
		}

		public FlowingFluid source() {
			return source;
		}

		public FlowingFluid flowing() {
			return flowing;
		}

		public LiquidBlock block() {
			return block;
		}

		public Item bucket() {
			return bucket;
		}

		int tickDelay() {
			return tickDelay;
		}

		int slope() {
			return slope;
		}

		int dropOff() {
			return dropOff;
		}
	}

	/** A gas: a fluid with no block or bucket (see {@link GasFluid}), and its gauge colour. */
	public record Gas(String id, Fluid fluid, int color) {
	}

	public static final Map<String, Entry> FLUIDS = new LinkedHashMap<>();
	public static final Map<String, Gas> GASES = new LinkedHashMap<>();
	/** Liquid crude oil: from reservoirs and oil sand; refined into fuels. */
	public static Entry CRUDE_OIL;
	/** Distillation fractions: light naphtha (for gasoline), diesel, and heavy fuel oil (for cracking). */
	public static Entry NAPHTHA;
	public static Entry DIESEL;
	public static Entry HEAVY_FUEL_OIL;
	/** Reformed naphtha: high-octane gasoline. */
	public static Entry GASOLINE;
	/** Vacuum distillation of heavy fuel oil: lubricant (machine upkeep). */
	public static Entry LUBRICANT;
	/** The lightest fraction: refinery gas (fuel gas, and later plastics). */
	public static Gas REFINERY_GAS;

	private PetroFluids() {
	}

	/** The entry for a petroleum fluid (source or flowing), or null for any other fluid. */
	public static @Nullable Entry of(Fluid fluid) {
		for (Entry entry : FLUIDS.values()) {
			if (fluid == entry.source || fluid == entry.flowing) {
				return entry;
			}
		}
		return null;
	}

	/** ARGB gauge colour for a petroleum fluid or gas, or 0 if it is neither. */
	public static int gaugeColor(Fluid fluid) {
		Entry entry = of(fluid);
		if (entry != null) {
			return entry.color();
		}
		for (Gas gas : GASES.values()) {
			if (gas.fluid() == fluid) {
				return gas.color();
			}
		}
		return 0;
	}

	public static void register() {
		CRUDE_OIL = fluid("crude_oil", 20, 2, 2, 0xFF1E1711, MapColor.COLOR_BLACK);
		NAPHTHA = fluid("naphtha", 5, 4, 1, 0xFFBEAA64, MapColor.COLOR_YELLOW);
		DIESEL = fluid("diesel", 8, 3, 1, 0xFFAA6E19, MapColor.COLOR_ORANGE);
		HEAVY_FUEL_OIL = fluid("heavy_fuel_oil", 30, 2, 2, 0xFF261E12, MapColor.COLOR_BLACK);
		LUBRICANT = fluid("lubricant", 25, 2, 2, 0xFF8C7D28, MapColor.COLOR_YELLOW);
		GASOLINE = fluid("gasoline", 4, 4, 1, 0xFFC86446, MapColor.COLOR_RED);
		REFINERY_GAS = gas("refinery_gas", 0xFFB8C4D0);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			for (Entry entry : FLUIDS.values()) {
				output.accept(entry.bucket());
			}
		});
	}

	private static Gas gas(String id, int color) {
		Gas gas = new Gas(id, Registry.register(BuiltInRegistries.FLUID, Jugcraft.id(id), new GasFluid()), color);
		GASES.put(id, gas);
		return gas;
	}

	private static Entry fluid(String id, int tickDelay, int slope, int dropOff, int gauge, MapColor color) {
		Entry entry = new Entry(id, tickDelay, slope, dropOff, gauge);
		entry.source = Registry.register(BuiltInRegistries.FLUID, Jugcraft.id(id), new OilFluid.Source(entry));
		entry.flowing = Registry.register(BuiltInRegistries.FLUID, Jugcraft.id("flowing_" + id), new OilFluid.Flowing(entry));
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		entry.block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				new LiquidBlock(entry.source, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(color).setId(blockKey)) {
				});
		ResourceKey<Item> bucketKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id + "_bucket"));
		entry.bucket = Registry.register(BuiltInRegistries.ITEM, bucketKey,
				new BucketItem(entry.source, new Item.Properties().setId(bucketKey).stacksTo(1).craftRemainder(Items.BUCKET)));
		FLUIDS.put(id, entry);
		return entry;
	}
}
