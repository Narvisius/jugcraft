package io.github.jimbozoomer.jugcraft.building;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

/** Natural sources run only during native chunk generation; no loaded-world retrofit. */
final class OwnerTwigsWorld {
    static void register() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, Jugcraft.id("twigworks_natural"), Natural.CODEC);
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Jugcraft.id("twigworks_leaf_twigs"), LeafTwigs.CODEC);
        try (var stream = OwnerTwigsWorld.class.getResourceAsStream("/jugcraft/newest_twigs_biomes.json")) {
            var additions = JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)).getAsJsonArray();
            for (var entry : additions) {
                var d = entry.getAsJsonObject(); String biome = d.get("biomes").getAsString();
                var selector = biome.startsWith("#") ? BiomeSelectors.tag(TagKey.create(Registries.BIOME, Identifier.parse(biome.substring(1))))
                    : BiomeSelectors.includeByKey(ResourceKey.create(Registries.BIOME, Identifier.parse(biome)));
                var features = d.get("features"); var values = features.isJsonArray() ? features.getAsJsonArray() : new JsonArray();
                if (!features.isJsonArray()) values.add(features);
                for (var f : values) BiomeModifications.addFeature(selector, GenerationStep.Decoration.valueOf(d.get("step").getAsString().toUpperCase(Locale.ROOT)), ResourceKey.create(Registries.PLACED_FEATURE, Identifier.parse(f.getAsString())));
            }
        } catch (IOException e) { throw new IllegalStateException("Twigs generation sources", e); }
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (!source.isBuiltin()) return;
            // Keep original leaf fortune/silk-touch conditions and exact counts.
            if (key.identifier().getPath().startsWith("blocks/")) table.apply(Holder.direct(new LeafTwigs()));
            if (key.identifier().toString().equals("minecraft:blocks/gravel"))
                table.withPool(LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1))).when(LootItemRandomChanceCondition.randomChance(.2F))
                    .add(LootItem.lootTableItem(OwnerTwigs.block("pebble")).apply(Holder.direct(new PebbleCount()))));
            if (key.identifier().toString().equals("minecraft:blocks/bamboo"))
                table.withPool(LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1))).add(LootItem.lootTableItem(OwnerTwigs.block("bamboo_leaves")).apply(Holder.direct(new BambooDrop()))));
        });
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Jugcraft.id("twigworks_pebble_count"), PebbleCount.CODEC);
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Jugcraft.id("twigworks_bamboo_drop"), BambooDrop.CODEC);
    }
    record LeafTwigs() implements LootItemFunction {
        static final MapCodec<LeafTwigs> CODEC = MapCodec.unit(LeafTwigs::new);
        @Override public MapCodec<LeafTwigs> codec() { return CODEC; }
        @Override public ItemStack apply(ItemStack stack, LootContext context) {
            var state = context.getOptional(LootContextParams.BLOCK_STATE);
            return state != null && state.getBlock() instanceof LeavesBlock && stack.is(Items.STICK) ? stack.transmuteCopy(OwnerTwigs.block("twig").asItem()) : stack;
        }
    }
    record PebbleCount() implements LootItemFunction {
        static final MapCodec<PebbleCount> CODEC = MapCodec.unit(PebbleCount::new);
        @Override public MapCodec<PebbleCount> codec() { return CODEC; }
        @Override public ItemStack apply(ItemStack stack, LootContext context) { return stack.copyWithCount(1 + context.getRandom().nextInt(3)); }
    }
    record BambooDrop() implements LootItemFunction {
        static final MapCodec<BambooDrop> CODEC = MapCodec.unit(BambooDrop::new);
        @Override public MapCodec<BambooDrop> codec() { return CODEC; }
        @Override public ItemStack apply(ItemStack stack, LootContext context) {
            var state = context.getOptional(LootContextParams.BLOCK_STATE);
            return state != null && state.hasProperty(BambooStalkBlock.LEAVES) && state.getValue(BambooStalkBlock.LEAVES) != BambooLeaves.NONE ? stack : ItemStack.EMPTY;
        }
    }
    record Natural(String kind) implements Feature {
        static final MapCodec<Natural> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.STRING.fieldOf("kind").forGetter(Natural::kind)).apply(i, Natural::new));
        @Override public MapCodec<Natural> codec() { return CODEC; }
        @Override public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
            if (kind.equals("silt_strip")) return strip(level, random, origin);
            if (kind.equals("azalea_flowers")) return flowers(level, random, origin);
            int tries = kind.equals("patch_twig") ? 3 : 2;
            boolean changed = false;
            for (int i = 0; i < tries; i++) {
                BlockPos pos = origin.offset(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8));
                if (!level.ensureCanWrite(pos)) continue;
                var old = level.getBlockState(pos);
                if (!old.isAir() && !old.is(Blocks.WATER)) continue;
                String name = kind.equals("patch_twig") ? "twig" : "pebble";
                if (kind.equals("patch_seashell")) { int choice = random.nextInt(63); name = choice < 20 ? "opaline_seashell" : choice < 40 ? "tangerine_seashell" : choice < 55 ? "roseate_seashell" : "bronzed_seashell"; }
                var state = OwnerTwigs.block(name).defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, old.getFluidState().getType() == Fluids.WATER);
                if (state.canSurvive(level, pos)) changed |= level.setBlock(pos, state, 2);
            }
            return changed;
        }
        private static boolean flowers(WorldGenLevel level, RandomSource random, BlockPos origin) {
            int radius = 2 + random.nextInt(3); boolean changed = false;
            var block = (MultifaceBlock)OwnerTwigs.block("azalea_flowers");
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -3, -radius), origin.offset(radius, 3, radius))) {
                if (!level.ensureCanWrite(pos) || random.nextInt(4) != 0) continue;
                var old = level.getBlockState(pos); if (!old.isAir() && !old.is(Blocks.WATER)) continue;
                var state = block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, old.is(Blocks.WATER));
                boolean attached = false;
                for (Direction direction : Direction.values()) {
                    var support = level.getBlockState(pos.relative(direction));
                    if (!support.is(Blocks.CLAY) && !support.is(Blocks.MOSS_BLOCK) && !support.is(Blocks.AZALEA_LEAVES) && !support.is(Blocks.FLOWERING_AZALEA_LEAVES)) continue;
                    if (MultifaceBlock.canAttachTo(level, direction, pos.relative(direction), support)) { state = state.setValue(MultifaceBlock.getFaceProperty(direction), true); attached = true; }
                }
                if (attached) changed |= level.setBlock(pos, state, 2);
            }
            return changed;
        }
        private static boolean strip(WorldGenLevel level, RandomSource random, BlockPos origin) {
            // Bounded geological ribbon. The supplied archive has its provider
            // data, but no implementation of the old noise_strip feature codec.
            boolean changed = false; double phase = random.nextDouble() * Math.PI * 2;
            for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
                if (x * x + z * z > 26 + random.nextInt(9)) continue;
                int y = (int)Math.round(Math.sin(x * .5 + phase) + Math.cos(z * .5 + phase));
                BlockPos pos = origin.offset(x, y, z);
                if (level.ensureCanWrite(pos) && level.getBlockState(pos).is(BlockTags.BASE_STONE_NETHER)) changed |= level.setBlock(pos, OwnerTwigs.block("silt").defaultBlockState(), 2);
            }
            return changed;
        }
    }
}
