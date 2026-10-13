package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * The orchards' fruit trees (the kitchen and cooking expansion's slice 6, tools/orchard.py TREES, in this order): each
 * grows from its seed (planted as {@code <id>_sapling}) into the tree {@code data/jugcraft/worldgen/feature/<id>_tree.json},
 * a vanilla oak trunk (the banana's is its own stem) under a crown of {@code <id>_leaves} ({@link OrchardLeavesBlock}), whose
 * ripe clusters give {@link #pickMin} to {@link #pickMax} of the fruit {@code <id>}. tools/check_mod_data.py compares this
 * with tools/orchard.py.
 */
public enum OrchardTree {
	PEAR("pear", "pear_seeds", 1, 3),
	PEACH("peach", "peach_pit", 1, 3),
	LEMON("lemon", "lemon_seeds", 1, 3),
	ORANGE("orange", "orange_seeds", 1, 3),
	PLUM("plum", "plum_pit", 1, 3),
	BANANA("banana", "banana_pup", 2, 4);

	/** The fruit's ID; the sapling, leaves and tree are named from it. */
	public final String id;
	/** The item that plants the sapling. */
	public final String seed;
	public final int pickMin;
	public final int pickMax;
	/** The tree the sapling grows (data/jugcraft/worldgen/feature/<id>_tree.json). */
	public final ResourceKey<Feature> feature;
	public final TreeGrower grower;

	OrchardTree(String id, String seed, int pickMin, int pickMax) {
		this.id = id;
		this.seed = seed;
		this.pickMin = pickMin;
		this.pickMax = pickMax;
		this.feature = ResourceKey.create(Registries.FEATURE, Jugcraft.id(id + "_tree"));
		this.grower = new TreeGrower(Jugcraft.MOD_ID + "_" + id, WeightedList.of(feature), WeightedList.of(), WeightedList.of(), feature);
	}

	public String sapling() {
		return id + "_sapling";
	}

	public String leaves() {
		return id + "_leaves";
	}
}
