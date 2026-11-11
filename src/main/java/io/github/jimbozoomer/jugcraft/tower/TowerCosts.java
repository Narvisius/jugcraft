package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * What the Drone Tower costs (since 26 October 2026, Justin: "the main 12 blocks" plus the holo parts): each step (the
 * foundation, then tiers 1 to 9) of the Tri-Pillar costs the {@link #CHARGED} blocks it places, as the items a player
 * would place them from: the twelve blocks the tower is mostly built of (the frozen earlier designs, with no foundation
 * step, still cost every block but the holograms). The holograms (Holograms, since 28 October 2026: no recipe, the
 * tower's own) come free in every design, as everything else it places does
 * (furniture, screens, lights, signs, pipes, the roof plant) comes free with the step. tools/tower_costs.py CHARGED is the
 * same list (check_mod_data compares them). Counting, as before: A fitting one item places whole (an exchange station, the master console, a
 * storage rack, the holo command table, the tower control console) counts once, at its first part; a door once, at its lower half; a
 * double slab twice; a console chair standing on the raised floor costs the chair and the floor tile it took. Parts that come
 * with another block and have no item of their own (a speed gate's pedestals, the parts of a video wall, a display case, a crystal tree, a long coffee table, the parts shelving, an aisle containment door or a holo projector) cost nothing.
 */
public final class TowerCosts {
	/** The blocks a step is paid for in (jugcraft ids): the twelve the tower is mostly built of. */
	public static final Set<String> CHARGED = Set.of(
			"carbon_composite_panel", "reinforced_concrete", "steel_armor_plate", "ceiling_tile", "access_floor_tile", "dock_plating",
			"framed_glass", "epoxy_floor", "hangar_pad", "neon_tube", "ceiling_light_panel", "painted_wall");

	private TowerCosts() {
	}

	/** Whether a step is paid for in {@code item} ({@link #CHARGED}); the rest comes free. */
	public static boolean charged(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		return id.getNamespace().equals(io.github.jimbozoomer.jugcraft.Jugcraft.MOD_ID) && CHARGED.contains(id.getPath());
	}

	/** How many of {@link #item(BlockState)} placing {@code state} takes: 0, 1 or 2. */
	public static int count(BlockState state) {
		Block block = state.getBlock();
		if (item(state) == Items.AIR) {
			return 0;
		}
		if (block instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
			return 0;
		}
		if (block instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
			return 2;
		}
		if (block instanceof RoomKitBlock kit) {
			return kit.isAnchor(state) ? 1 : 0;                  // a room kit piece: once, at its anchor
		}
		if (block instanceof StructureGhost.Source || block instanceof MasterConsoleBlock || block instanceof HoloCommandTableBlock
				|| block instanceof TowerControlConsoleBlock) {
			for (Property<?> property : state.getProperties()) {
				if (property instanceof IntegerProperty part && property.getName().equals("part")) {
					return state.getValue(part) == 0 ? 1 : 0;
				}
			}
		}
		return 1;
	}

	/** Whether {@code item} is a hologram's (Holograms): never paid for, the tower puts them in itself. */
	static boolean hologram(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		return id.getNamespace().equals(io.github.jimbozoomer.jugcraft.Jugcraft.MOD_ID) && Holograms.ITEMS.contains(id.getPath());
	}

	/** The item a block is placed from (air when it has none). */
	public static Item item(BlockState state) {
		return state.getBlock().asItem();
	}

	/** The items {@code step} of {@code data} is built from, most needed first. */
	static Map<Item, Integer> of(TowerData data, TowerData.Tier step) {
		Map<Item, Integer> counts = new HashMap<>();
		// the Tri-Pillar (the layouts with a foundation) is paid in CHARGED only; the frozen earlier designs as they were
		boolean all = data.foundation == null;
		for (int[] b : step.place) {
			BlockState state = data.state(b[3]);
			int n = count(state);
			if (n > 0 && (all || charged(item(state))) && !hologram(item(state))) {
				counts.merge(item(state), n, Integer::sum);
			}
			if (state.getBlock() instanceof ConsoleChairBlock && state.getValue(ConsoleChairBlock.PLATFORM)
					&& (all || charged(JugcraftTower.BLOCKS.get("raised_floor").asItem()))) {
				// a chair on the raised floor took the floor's cell: it is placed from both
				counts.merge(JugcraftTower.BLOCKS.get("raised_floor").asItem(), 1, Integer::sum);
			}
		}
		List<Map.Entry<Item, Integer>> sorted = new ArrayList<>(counts.entrySet());
		sorted.sort((a, b) -> a.getValue().equals(b.getValue())
				? BuiltInRegistries.ITEM.getKey(a.getKey()).compareTo(BuiltInRegistries.ITEM.getKey(b.getKey()))
				: Integer.compare(b.getValue(), a.getValue()));
		Map<Item, Integer> out = new LinkedHashMap<>();
		for (var entry : sorted) {
			out.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(out);
	}

	/** Every item in {@code cost}, added up. */
	public static int total(Map<Item, Integer> cost) {
		int sum = 0;
		for (int n : cost.values()) {
			sum += n;
		}
		return sum;
	}
}
