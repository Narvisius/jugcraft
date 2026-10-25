package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Function;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.Fluids;

/** Supplied quartz/sky-stone building sets and a renewable, staged certus growing route. */
public final class DigitalMaterials {
    public static final String[][] FAMILIES = {
        {"quartz", "quartz_block"}, {"quartz_brick", "quartz_bricks"}, {"chiseled_quartz", "chiseled_quartz_block"},
        {"cut_quartz", "cut_quartz_block"}, {"smooth_quartz", "smooth_quartz_block"}, {"quartz_pillar", "quartz_pillar"},
        {"fluix", "fluix_block"}, {"sky_stone", "sky_stone_block"}, {"sky_stone_brick", "sky_stone_brick"},
        {"sky_stone_small_brick", "sky_stone_small_brick"}, {"smooth_sky_stone", "smooth_sky_stone_block"}
    };
    private DigitalMaterials() {}
    public static void register() {
        for (var family : FAMILIES) {
            Block base = add(family[1], p -> family[1].equals("quartz_pillar") ? new RotatedPillarBlock(p) : new Block(p), false);
            add(family[0] + "_stairs", p -> new Stairs(base.defaultBlockState(), p), false);
            add(family[0] + "_slab", SlabBlock::new, false);
            add(family[0] + "_wall", WallBlock::new, false);
        }
        for (String grade : new String[]{"damaged", "chipped", "flawed", "flawless"}) add(grade + "_budding_quartz", p -> new Budding(p.randomTicks()), false);
        add("small_quartz_bud", p -> new AmethystClusterBlock(3, 4, p), true);
        add("medium_quartz_bud", p -> new AmethystClusterBlock(4, 3, p), true);
        add("large_quartz_bud", p -> new AmethystClusterBlock(5, 3, p), true);
        add("quartz_cluster", p -> new AmethystClusterBlock(7, 3, p), true);
    }
    private static Block add(String name, Function<BlockBehaviour.Properties, Block> factory, boolean bud) {
        var id = Jugcraft.id("jimta_" + name); var key = ResourceKey.create(Registries.BLOCK, id);
        var props = BlockBehaviour.Properties.ofFullCopy(bud ? Blocks.AMETHYST_CLUSTER : Blocks.QUARTZ_BLOCK).setId(key);
        Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props)); DigitalStorage.BLOCKS.put(name, block);
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }
    private static final class Stairs extends StairBlock { Stairs(BlockState base, Properties p) { super(base, p); } }
    public static final class Budding extends Block {
        Budding(Properties p) { super(p); }
        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(5) != 0) return;
            Direction face = Direction.getRandom(random); BlockPos target = pos.relative(face);
            if (!level.isLoaded(target)) return;
            var old = level.getBlockState(target); Block next = null;
            if (BuddingAmethystBlock.canClusterGrowAtState(old)) next = DigitalStorage.BLOCKS.get("small_quartz_bud");
            else if (old.hasProperty(AmethystClusterBlock.FACING) && old.getValue(AmethystClusterBlock.FACING) == face) {
                if (old.is(DigitalStorage.BLOCKS.get("small_quartz_bud"))) next = DigitalStorage.BLOCKS.get("medium_quartz_bud");
                else if (old.is(DigitalStorage.BLOCKS.get("medium_quartz_bud"))) next = DigitalStorage.BLOCKS.get("large_quartz_bud");
                else if (old.is(DigitalStorage.BLOCKS.get("large_quartz_bud"))) next = DigitalStorage.BLOCKS.get("quartz_cluster");
            }
            if (next == null) return;
            level.setBlockAndUpdate(target, next.defaultBlockState().setValue(AmethystClusterBlock.FACING, face)
                    .setValue(AmethystClusterBlock.WATERLOGGED, old.getFluidState().getType() == Fluids.WATER));
            if (next == DigitalStorage.BLOCKS.get("quartz_cluster") && random.nextInt(12) == 0) {
                String nextGrade = state.is(DigitalStorage.BLOCKS.get("flawed_budding_quartz")) ? "chipped_budding_quartz"
                        : state.is(DigitalStorage.BLOCKS.get("chipped_budding_quartz")) ? "damaged_budding_quartz"
                        : state.is(DigitalStorage.BLOCKS.get("damaged_budding_quartz")) ? "quartz_block" : null;
                if (nextGrade != null) level.setBlockAndUpdate(pos, DigitalStorage.BLOCKS.get(nextGrade).defaultBlockState());
            }
        }
    }
}
