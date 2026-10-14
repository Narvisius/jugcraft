package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials;
import io.github.jimbozoomer.jugcraft.materials.SurfaceLinkedOreFeature;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import java.util.List;
import java.util.Collections;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class SpodumeneGameTests {
	@GameTest public void miningAndStorage(GameTestHelper helper) {
		var family = JugcraftMaterials.SPODUMENE;
		var level = helper.getLevel();
		var iron = new ItemStack(Items.IRON_PICKAXE);
		var silk = iron.copy();
		silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		var fortune = iron.copy();
		fortune.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
		for (Block ore : List.of(family.ore, family.deepslateOre)) {
			var state = ore.defaultBlockState();
			helper.assertTrue(iron.isCorrectToolForDrops(state), "Iron pickaxe cannot mine spodumene");
			helper.assertTrue(!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state), "Stone pickaxe bypassed iron tier");
			helper.assertTrue(state.is(TagKey.create(Registries.BLOCK, Identifier.parse("c:ores"))), "Ore drill tag missing");
			var pos = helper.absolutePos(new BlockPos(1, 1, 1));
			var plain = Block.getDrops(state, level, pos, null, null, iron);
			helper.assertTrue(plain.size() == 1 && plain.getFirst().is(family.item) && plain.getFirst().getCount() == 1, "Plain spodumene drops: " + plain);
			var preserved = Block.getDrops(state, level, pos, null, null, silk);
			helper.assertTrue(preserved.size() == 1 && preserved.getFirst().is(ore.asItem()), "Silk Touch did not preserve the ore");
			int most = 0;
			for (int n = 0; n < 64; n++) {
				var drops = Block.getDrops(state, level, pos, null, null, fortune);
				helper.assertTrue(drops.size() == 1 && drops.getFirst().is(family.item) && drops.getFirst().getCount() <= 4, "Invalid Fortune drops");
				most = Math.max(most, drops.getFirst().getCount());
			}
			helper.assertTrue(most > 1, "Fortune never increased spodumene");
			var crushed = MachineRecipes.find(level, MachineKind.CRUSHER, new ItemStack(ore.asItem()))
					.orElseThrow(() -> helper.assertionException("Missing spodumene crushing recipe")).output().create();
			helper.assertTrue(crushed.is(family.item) && crushed.getCount() == 2, "Ore did not crush into two spodumene");
		}
		var recipes = level.getServer().getRecipeManager();
		var nine = CraftingInput.of(3, 3, Collections.nCopies(9, new ItemStack(family.item)));
		var compressed = recipes.getRecipeFor(RecipeType.CRAFTING, nine, level).orElseThrow().value().assemble(nine);
		helper.assertTrue(compressed.is(family.storageBlock.asItem()) && compressed.getCount() == 1, "Compaction failed");
		var one = CraftingInput.of(1, 1, List.of(compressed));
		var unpacked = recipes.getRecipeFor(RecipeType.CRAFTING, one, level).orElseThrow().value().assemble(one);
		helper.assertTrue(unpacked.is(family.item) && unpacked.getCount() == 9, "Unpacking failed");
		helper.succeed();
	}

	@GameTest public void surfacePolicyAndData(GameTestHelper helper) {
		var level = helper.getLevel();
		var feature = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Jugcraft.id("ore_spodumene"));
		helper.assertTrue(feature instanceof SurfaceLinkedOreFeature, "Wrong feature codec");
		var linked = (SurfaceLinkedOreFeature) feature;
		helper.assertTrue(linked.ore().size() == 40 && linked.ore().discardChanceOnAirExposure() == 0 && linked.outcropChance() == 4, "Wrong balance data");
		var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		helper.assertTrue(biomes.getOrThrow(linked.surfaceBiomes()).size() == 7, "Wrong eligible biome count");
		for (String id : List.of("minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:stony_peaks", "jugcraft:shield", "jugcraft:highland", "jugcraft:basin")) {
			helper.assertTrue(biomes.get(Identifier.parse(id)).orElseThrow().is(linked.surfaceBiomes()), "Missing rocky biome " + id);
		}
		for (String id : List.of("minecraft:taiga", "minecraft:snowy_plains", "minecraft:snowy_beach", "minecraft:frozen_river", "minecraft:frozen_ocean",
				"minecraft:lush_caves", "jugcraft:alpine_spawn", "jugcraft:tundra", "jugcraft:cold_desert", "jugcraft:ice_sheet")) {
			helper.assertTrue(!biomes.get(Identifier.parse(id)).orElseThrow().is(linked.surfaceBiomes()), "Wrong eligible biome " + id);
		}
		helper.assertTrue(JugcraftConfig.FEATURES.contains("lithium"), "Missing feature switch");
		helper.succeed();
	}

	@GameTest(maxTicks = 400) public void prospectAndDrill(GameTestHelper helper) {
		var family = JugcraftMaterials.SPODUMENE;
		var ore = new BlockPos(5, 1, 3); var deep = new BlockPos(2, 1, 6); var master = new BlockPos(4, 2, 4);
		helper.setBlock(ore, family.ore); helper.setBlock(deep, family.deepslateOre);
		var readings = OreSurvey.survey(helper.getLevel(), helper.absolutePos(master), helper.getLevel().getRandom(), 0, 1);
		helper.assertTrue(readings.stream().anyMatch(r -> r.icon().equals(Jugcraft.id("spodumene_ore"))), "Prospector missed spodumene");
		var block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.ORE_DRILL);
		helper.setBlock(master, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		var drill = helper.getBlockEntity(master, MachineBlockEntity.class);
		var energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.EAST);
		energy.setAmount(energy.getCapacity());
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.STONE, ore); helper.assertBlockPresent(Blocks.DEEPSLATE, deep);
			int normal = 0, deepslate = 0;
			for (int slot = 0; slot < MachineKind.ORE_DRILL.slots; slot++) {
				var stack = drill.getItem(slot);
				if (stack.is(family.ore.asItem())) normal += stack.getCount();
				if (stack.is(family.deepslateOre.asItem())) deepslate += stack.getCount();
			}
			helper.assertTrue(normal == 1 && deepslate == 1, "Drill did not recover both finite spodumene ore blocks");
		});
	}
}
