package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials;
import io.github.jimbozoomer.jugcraft.materials.MineralSandFeature;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class MineralSandGameTests {
    static List<Block> sands() { return List.of(JugcraftMaterials.TITANIUM_SAND, JugcraftMaterials.ZIRCON_SAND,
            JugcraftMaterials.RARE_EARTH_SAND, JugcraftMaterials.GARNET_SAND); }
    static List<String> names() { return List.of("titanium_sand", "zircon_sand", "rare_earth_sand", "garnet_sand"); }

    @GameTest public void fixedYieldAndFamilyIdentity(GameTestHelper helper) {
        var level = helper.getLevel();
        var plain = new ItemStack(Items.IRON_SHOVEL);
        var silk = plain.copy(); var fortune = plain.copy();
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        silk.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        fortune.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), 3);
        for (Block sand : sands()) {
            var state = sand.defaultBlockState();
            helper.assertTrue(state.is(net.minecraft.tags.BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP)
                    && state.is(net.minecraft.tags.BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP_NO_LEAVES), "Mineral sediment missing 26.3 ground-heightmap tags");
            helper.assertTrue(state.isCollisionShapeFullBlock(level, helper.absolutePos(new BlockPos(1, 1, 1))), "Mineral sediment must support players");
            var surface = helper.absolutePos(new BlockPos(1, 12, 1));
            level.setBlock(surface, state, 3);
            helper.assertTrue(level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, surface.getX(), surface.getZ()) == surface.getY() + 1,
                    "Heightmap ignored an actual placed reserve block");
            level.setBlock(surface, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(state.is(OreDrilling.MINERAL_SANDS) && !state.is(OreDrilling.ORES), "Incorrect reserve/ore-yield tags");
            for (var tool : List.of(plain, silk, fortune, ItemStack.EMPTY)) for (int n = 0; n < 32; n++) {
                var drops = Block.getDrops(state, level, helper.absolutePos(new BlockPos(1, 1, 1)), null, null, tool);
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(sand.asItem()) && drops.getFirst().getCount() == 1,
                        "Reserve multiplied or lost family: " + drops);
            }
            for (var machine : List.of(MachineKind.CRUSHER, MachineKind.PULVERIZER)) {
                helper.assertTrue(MachineRecipes.find(level, machine, new ItemStack(sand)).isEmpty(), "Sand acquired generic ore bonus processing");
            }
        }
        for (int i = 0; i < 4; i++) {
            var feature = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Jugcraft.id("ore_" + names().get(i)));
            helper.assertTrue(feature instanceof MineralSandFeature, "Missing sand codec");
            var lens = (MineralSandFeature) feature;
            helper.assertTrue(lens.family() == i && lens.sand().is(sands().get(i)), "Swapped family configuration");
            var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
            for (String excluded : List.of("minecraft:lush_caves", "minecraft:snowy_plains", "minecraft:deep_ocean", "minecraft:plains")) {
                helper.assertTrue(!biomes.get(net.minecraft.resources.Identifier.parse(excluded)).orElseThrow().is(lens.surfaceBiomes()), "Sand in excluded biome");
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 240) public void titaniumFiniteDrill(GameTestHelper h) { drill(h, 0); }
    @GameTest(maxTicks = 240) public void zirconFiniteDrill(GameTestHelper h) { drill(h, 1); }
    @GameTest(maxTicks = 240) public void rareEarthFiniteDrill(GameTestHelper h) { drill(h, 2); }
    @GameTest(maxTicks = 240) public void garnetFiniteDrill(GameTestHelper h) { drill(h, 3); }

    private static void drill(GameTestHelper helper, int family) {
        var sand = sands().get(family); var level = helper.getLevel();
        var target = new BlockPos(5, 1, 3); var master = new BlockPos(4, 2, 4);
        helper.setBlock(target.below(), Blocks.STONE); helper.setBlock(target, sand);
        var readings = OreSurvey.survey(level, helper.absolutePos(master), level.getRandom(), 1, 1);
        helper.assertTrue(readings.stream().anyMatch(r -> r.icon().equals(Jugcraft.id(names().get(family)))), "Prospector missed sand family");
        var block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.ORE_DRILL);
        helper.setBlock(master, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        block.setPlacedBy(level, helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
        var drill = helper.getBlockEntity(master, MachineBlockEntity.class);
        var energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(level, helper.absolutePos(master), Direction.EAST);
        energy.setAmount(energy.getCapacity());
        for (int slot = MachineKind.ORE_DRILL.outputSlot(); slot < MachineKind.ORE_DRILL.slots; slot++) drill.setItem(slot, new ItemStack(Items.STONE, 64));
        helper.runAtTickTime(60, () -> {
            helper.assertBlockPresent(sand, target);
            helper.assertTrue(energy.getAmount() == energy.getCapacity(), "Full output spent energy or reserve");
            for (int slot = MachineKind.ORE_DRILL.outputSlot(); slot < MachineKind.ORE_DRILL.slots; slot++) drill.setItem(slot, ItemStack.EMPTY);
        });
        helper.runAtTickTime(200, () -> {
            helper.assertBlockPresent(Blocks.SAND, target);
            int recovered = 0;
            for (int slot = 0; slot < MachineKind.ORE_DRILL.slots; slot++) if (drill.getItem(slot).is(sand.asItem())) recovered += drill.getItem(slot).getCount();
            helper.assertTrue(recovered == 1, "Repeated ticks did not recover exactly one finite unit: " + recovered);
            helper.assertTrue(!OreDrilling.isOre(helper.getBlockState(target)), "Depleted filler remains extractable");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 240) public void overlappingDrillsShareOneReserve(GameTestHelper helper) {
        var target = new BlockPos(4, 1, 4); var sand = sands().getFirst();
        helper.setBlock(target.below(), Blocks.STONE); helper.setBlock(target, sand);
        var drills = new java.util.ArrayList<MachineBlockEntity>();
        for (var master : List.of(new BlockPos(2, 2, 4), new BlockPos(6, 2, 4))) {
            var block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.ORE_DRILL);
            helper.setBlock(master, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
            block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
            drills.add(helper.getBlockEntity(master, MachineBlockEntity.class));
            var energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.EAST);
            energy.setAmount(energy.getCapacity());
        }
        helper.runAtTickTime(200, () -> {
            helper.assertBlockPresent(Blocks.SAND, target);
            int recovered = 0;
            for (var drill : drills) for (int slot = 0; slot < MachineKind.ORE_DRILL.slots; slot++)
                if (drill.getItem(slot).is(sand.asItem())) recovered += drill.getItem(slot).getCount();
            helper.assertTrue(recovered == 1, "Overlapping extraction duplicated reserve: " + recovered);
            helper.succeed();
        });
    }
}
