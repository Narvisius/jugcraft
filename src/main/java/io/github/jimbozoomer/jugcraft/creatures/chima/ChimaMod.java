package io.github.jimbozoomer.jugcraft.creatures.chima;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Chima the lion cub ({@link Chima}, docs/features/chima.md): its entity type, spawn egg and natural spawning. Chimas
 * come to savannas (biome tag {@code c:is_savanna}), one or two at a time at weight {@value #SPAWN_WEIGHT}, on grass in
 * daylight, like the other animals there.
 */
public final class ChimaMod {
	public static final int SPAWN_WEIGHT = 6;
	public static final EntityType<Chima> CHIMA = Registry.register(BuiltInRegistries.ENTITY_TYPE, Jugcraft.id("chima"),
			EntityType.Builder.<Chima>of(Chima::new, MobCategory.CREATURE).sized(0.7F, 0.95F).eyeHeight(0.7F).clientTrackingRange(10)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("chima"))));
	public static final Item SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, Jugcraft.id("chima_spawn_egg"),
			new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Jugcraft.id("chima_spawn_egg"))).spawnEgg(CHIMA)));

	private ChimaMod() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(CHIMA, Chima.createAttributes());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(SPAWN_EGG));
		BiomeModifications.addSpawn(BiomeSelectors.tag(ConventionalBiomeTags.IS_SAVANNA), MobCategory.CREATURE, CHIMA, SPAWN_WEIGHT, 1, 2);
		SpawnPlacements.register(CHIMA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ChimaMod::grassland);
	}

	/** Natural spawns stand on grass (what animals spawn on) in daylight; eggs and commands go anywhere. */
	public static boolean grassland(EntityType<Chima> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
			return true;
		}
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && level.getRawBrightness(pos, 0) > 8;
	}
}
