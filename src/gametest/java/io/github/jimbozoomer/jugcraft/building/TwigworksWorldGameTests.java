package io.github.jimbozoomer.jugcraft.building;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.menagerie.HouseholdBlock;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

/** Exercise the real registered blocks and feature placement, not descriptor names alone. */
public final class TwigworksWorldGameTests {
    @GameTest
    public void suppliedWaterloggingUsesNativePropertyAndSurvivesSave(GameTestHelper h) {
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof HouseholdBlock)) continue;
            var property = block.getStateDefinition().getProperty("waterlogged");
            if (property == null) continue;
            h.assertTrue(property == BlockStateProperties.WATERLOGGED,
                "Native waterlogging property required for " + BuiltInRegistries.BLOCK.getKey(block));
            for (boolean wet : new boolean[]{false, true}) {
                var state = block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet);
                var saved = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
                h.assertTrue(BlockState.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow() == state,
                    "Saved waterlogging round-trips without changing block/state IDs");
            }
            checked++;
        }
        h.assertTrue(checked > 6, "Audit covers the shared collection, not only the pebble");
        h.succeed();
    }

    @GameTest
    public void naturalPebblesTwigsAndEveryShellPlaceDryAndUnderwater(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(8, 30, 8));
        Set<String> seen = new HashSet<>();
        for (boolean wet : new boolean[]{false, true}) {
            for (String kind : new String[]{"patch_pebble", "patch_twig", "patch_seashell"}) {
                prepare(h, origin, wet, Blocks.STONE);
                var feature = new OwnerTwigsWorld.Natural(kind);
                for (int seed = 0; seed < 128; seed++)
                    feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin);
                int count = 0;
                for (var pos : BlockPos.betweenClosed(origin.offset(-7, 0, -7), origin.offset(7, 0, 7))) {
                    var state = level.getBlockState(pos);
                    String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                    if (!id.startsWith("twigworks_")) continue;
                    h.assertTrue(state.getValue(BlockStateProperties.WATERLOGGED) == wet, "Placed waterlogging matches the replaced fluid");
                    h.assertTrue(state.getFluidState().is(Fluids.WATER) == wet, "Underwater features preserve source water");
                    h.assertTrue(state.canSurvive(level, pos), "Generated decoration has valid support");
                    seen.add(wet + ":" + id); count++;
                }
                h.assertTrue(count > 0, "Natural feature actually placed blocks: " + kind + " wet=" + wet);
            }
        }
        for (boolean wet : new boolean[]{false, true})
            for (String name : new String[]{"pebble", "twig", "opaline_seashell", "tangerine_seashell", "roseate_seashell", "bronzed_seashell"})
                h.assertTrue(seen.contains(wet + ":twigworks_" + name), "All natural variants exercised: " + name + " wet=" + wet);
        h.succeed();
    }

    @GameTest
    public void nativeBucketsFillAndDrainNaturalDecorations(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
        for (String name : new String[]{"pebble", "twig", "opaline_seashell", "tangerine_seashell", "roseate_seashell", "bronzed_seashell"}) {
            var block = OwnerTwigs.block(name); var liquid = (SimpleWaterloggedBlock)block;
            var dry = block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
            level.setBlock(pos, dry, 2);
            h.assertTrue(liquid.placeLiquid(level, pos, dry, Fluids.WATER.getSource(false)), "Native filling works for " + name);
            var wet = level.getBlockState(pos);
            h.assertTrue(wet.getFluidState().is(Fluids.WATER), "Filled decoration contains water");
            h.assertTrue(liquid.pickupBlock(null, level, pos, wet).is(Items.WATER_BUCKET), "Native draining returns one water bucket");
            h.assertTrue(level.getBlockState(pos) == dry, "Draining preserves decoration and restores dry state");
        }
        h.succeed();
    }

    @GameTest
    public void azaleaFlowerFeatureSupportsDryAndWetAttachment(GameTestHelper h) {
        var level = h.getLevel(); var origin = h.absolutePos(new BlockPos(8, 30, 8));
        for (boolean wet : new boolean[]{false, true}) {
            prepare(h, origin, wet, Blocks.MOSS_BLOCK);
            h.assertTrue(new OwnerTwigsWorld.Natural("azalea_flowers").place(level,
                level.getChunkSource().getGenerator(), RandomSource.create(5), origin), "Flowers attach in both environments");
            int count = 0;
            for (var pos : BlockPos.betweenClosed(origin.offset(-4, 0, -4), origin.offset(4, 0, 4))) {
                var state = level.getBlockState(pos);
                if (!state.is(OwnerTwigs.block("azalea_flowers"))) continue;
                h.assertTrue(state.getValue(BlockStateProperties.WATERLOGGED) == wet, "Flowers preserve water");
                h.assertTrue(state.canSurvive(level, pos), "Flowers retain a supported face"); count++;
            }
            h.assertTrue(count > 0, "Expected attached azalea flower blocks");
        }
        h.succeed();
    }

    private static void prepare(GameTestHelper h, BlockPos origin, boolean wet, Block floor) {
        for (var pos : BlockPos.betweenClosed(origin.offset(-8, -3, -8), origin.offset(8, 4, 8))) {
            var state = pos.getY() < origin.getY() ? floor.defaultBlockState()
                : wet && pos.getY() == origin.getY() ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            h.getLevel().setBlock(pos, state, 2);
        }
    }
}
