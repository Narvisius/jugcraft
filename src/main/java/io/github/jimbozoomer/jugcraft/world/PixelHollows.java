package io.github.jimbozoomer.jugcraft.world;

import com.mojang.datafixers.util.Pair;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.materials.JugcraftRegistry;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;

/**
 * Pixel Hollows: a rare cave biome deep under dry land that looks like the inside of an old console.
 * Circuitstone walls, faintly glowing pixel crystal clusters, and 1.5x the usual copper and redstone.
 * The biome and its features are data (data/jugcraft/worldgen, generated from tools/pixel_hollows.py).
 *
 * <p>The biome joins the Overworld's climate table through {@code mixin/OverworldBiomeBuilderMixin}, because
 * Fabric API has no Overworld biome API. The {@code pixel_hollows} feature switch stops new generation (and the
 * recipes); the blocks, items and biome stay registered, so builds and old chunks are untouched.
 */
public final class PixelHollows {
	public static final String FEATURE = "pixel_hollows";
	public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, Jugcraft.id("pixel_hollows"));

	/**
	 * Where the biome sits in the climate table. Deep (the depth band of the vanilla cave biomes, minus its top
	 * quarter) under the driest land that is not ocean, and short of the far inland (continentalness 0.8 and up),
	 * where vanilla's dripstone caves sit over the same depths: two biomes at the same climate distance would leave
	 * the winner to the climate search's cache. The search for the trader's map relies on the same band. Its share and
	 * spacing in a world have not been measured.
	 */
	public static final Climate.ParameterPoint PARAMETERS = Climate.parameters(
			Climate.Parameter.span(-1.0F, 1.0F), // temperature
			Climate.Parameter.span(-1.0F, -0.6F), // humidity: the driest land
			Climate.Parameter.span(-0.11F, 0.8F), // continentalness: coast and inland, not under the sea or the far inland
			Climate.Parameter.span(-1.0F, 1.0F), // erosion
			Climate.Parameter.span(0.3F, 0.9F), // depth: about 40 to 115 blocks below the surface
			Climate.Parameter.span(-1.0F, 1.0F), // weirdness
			0.0F);

	public static Block CIRCUITSTONE;
	public static Block POLISHED_CIRCUITSTONE;
	public static Block CIRCUITSTONE_BRICKS;
	public static Block PIXEL_LAMP;
	public static Block PIXEL_CRYSTAL_CLUSTER;
	public static Item PIXEL_SHARD;
	public static SoundEvent AMBIENT_LOOP;
	public static SoundEvent AMBIENT_ADDITIONS;

	private PixelHollows() {
	}

	public static void register() {
		CIRCUITSTONE = JugcraftRegistry.block("circuitstone", Blocks.DEEPSLATE);
		POLISHED_CIRCUITSTONE = JugcraftRegistry.block("polished_circuitstone", Blocks.POLISHED_DEEPSLATE);
		CIRCUITSTONE_BRICKS = JugcraftRegistry.block("circuitstone_bricks", Blocks.DEEPSLATE_BRICKS);
		PIXEL_LAMP = JugcraftRegistry.block("pixel_lamp", Blocks.SEA_LANTERN);

		// Static: no growth, no random ticks, no block entity. Faint light (3), so dark pockets still spawn mobs.
		ResourceKey<Block> clusterKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("pixel_crystal_cluster"));
		PIXEL_CRYSTAL_CLUSTER = Registry.register(BuiltInRegistries.BLOCK, clusterKey, new AmethystClusterBlock(13.0F, 2.0F,
				BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel(state -> 3).setId(clusterKey)));
		ResourceKey<Item> clusterItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("pixel_crystal_cluster"));
		Registry.register(BuiltInRegistries.ITEM, clusterItem,
				new BlockItem(PIXEL_CRYSTAL_CLUSTER, new Item.Properties().setId(clusterItem).useBlockDescriptionPrefix()));
		PIXEL_SHARD = JugcraftRegistry.item("pixel_shard");

		AMBIENT_LOOP = sound("ambient.pixel_hollows.loop");
		AMBIENT_ADDITIONS = sound("ambient.pixel_hollows.additions");

		// The biome lists its own copper and redstone bonus; the tin bonus exists only while tin does.
		if (JugcraftConfig.isFeatureEnabled(FEATURE) && JugcraftConfig.isFeatureEnabled("tin")) {
			BiomeModifications.addFeature(BiomeSelectors.includeByKey(BIOME), GenerationStep.Decoration.UNDERGROUND_ORES,
					ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("pixel_hollows_tin")));
		}

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
			output.accept(CIRCUITSTONE);
			output.accept(POLISHED_CIRCUITSTONE);
			output.accept(CIRCUITSTONE_BRICKS);
			output.accept(PIXEL_LAMP);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> {
			output.accept(CIRCUITSTONE);
			output.accept(PIXEL_CRYSTAL_CLUSTER);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(PIXEL_SHARD));
	}

	/** Called by the Overworld biome builder (mixin): adds the biome to the climate table unless switched off. */
	public static void addToOverworld(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
		if (JugcraftConfig.isFeatureEnabled(FEATURE)) {
			biomes.accept(Pair.of(PARAMETERS, BIOME));
		}
	}

	private static SoundEvent sound(String path) {
		Identifier id = Jugcraft.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}
}
