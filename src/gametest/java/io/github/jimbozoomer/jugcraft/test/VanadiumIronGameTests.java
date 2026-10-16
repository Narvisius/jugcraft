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

public class VanadiumIronGameTests {
	@GameTest public void miningAndStorage(GameTestHelper helper) {
		var family = JugcraftMaterials.VANADIUM_IRON;
		var level = helper.getLevel();
		var iron = new ItemStack(Items.IRON_PICKAXE);
		var silk = iron.copy();
		silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		var fortune = iron.copy();
		fortune.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
		for (Block ore : List.of(family.ore, family.deepslateOre)) {
			var state = ore.defaultBlockState();
			helper.assertTrue(iron.isCorrectToolForDrops(state), "Iron pickaxe cannot mine vanadium_iron");
			helper.assertTrue(!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state), "Stone pickaxe bypassed iron tier");
			helper.assertTrue(state.is(TagKey.create(Registries.BLOCK, Identifier.parse("c:ores"))), "Ore drill tag missing");
			var pos = helper.absolutePos(new BlockPos(1, 1, 1));
			var plain = Block.getDrops(state, level, pos, null, null, iron);
			helper.assertTrue(plain.size() == 1 && plain.getFirst().is(family.item) && plain.getFirst().getCount() == 1, "Plain vanadium_iron drops: " + plain);
			var preserved = Block.getDrops(state, level, pos, null, null, silk);
			helper.assertTrue(preserved.size() == 1 && preserved.getFirst().is(ore.asItem()), "Silk Touch did not preserve the ore");

			for (int n = 0; n < 64; n++) {
				var drops = Block.getDrops(state, level, pos, null, null, fortune);
				helper.assertTrue(drops.size() == 1 && drops.getFirst().is(family.item) && drops.getFirst().getCount() == 1, "Invalid Fortune drops");

			}

			var crushed = MachineRecipes.find(level, MachineKind.CRUSHER, new ItemStack(ore.asItem()))
					.orElseThrow(() -> helper.assertionException("Missing vanadium_iron crushing recipe")).output().create();
			helper.assertTrue(crushed.is(item("vanadium_iron_concentrate")) && crushed.getCount() == 1, "Ore must crush into exactly one concentrate");
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
		var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		for (boolean rich : List.of(false, true)) {
			var linked = (SurfaceLinkedOreFeature) level.registryAccess().lookupOrThrow(Registries.FEATURE)
					.getValue(Jugcraft.id(rich ? "ore_vanadium_iron_rich" : "ore_vanadium_iron"));
			helper.assertTrue(linked.ore().size() == (rich ? 48 : 40) && linked.outcropChance() == 3
					&& linked.ore().discardChanceOnAirExposure() == 0, "Wrong deposit balance");
			var expected = rich ? List.of("jugcraft:shield", "jugcraft:volcano") : List.of("jugcraft:highland", "jugcraft:basin",
					"minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:stony_peaks");
			helper.assertTrue(biomes.getOrThrow(linked.surfaceBiomes()).size() == expected.size(), "Wrong biome count");
			for (String id : expected) helper.assertTrue(biomes.get(Identifier.parse(id)).orElseThrow().is(linked.surfaceBiomes()), "Missing " + id);
			if (rich) {
				helper.assertTrue(biomes.getOrThrow(linked.outcropBiomes().orElseThrow()).size() == 1
						&& biomes.get(Jugcraft.id("shield")).orElseThrow().is(linked.outcropBiomes().orElseThrow()), "Only Shield permits primary outcrops");
			}
		}
		for (var block : List.of(JugcraftMaterials.VANADIUM_IRON.ore, JugcraftMaterials.VANADIUM_IRON.deepslateOre)) {
			helper.assertFalse(block.defaultBlockState().is(TagKey.create(Registries.BLOCK, Identifier.parse("c:ores/iron"))), "Vanadium leaked into ordinary iron processing");
		}
		for (String id : List.of("vanadium_iron_concentrate", "chemical_reaction/bulk_vanadium_pentoxide")) {
			helper.assertTrue(level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isEmpty(), "Legacy iron shortcut enabled by default: " + id);
		}
		helper.assertTrue(JugcraftConfig.FEATURES.contains("vanadium"), "Missing switch");
		helper.succeed();
	}

	@GameTest(maxTicks = 400) public void prospectAndDrill(GameTestHelper helper) {
		var family = JugcraftMaterials.VANADIUM_IRON;
		var ore = new BlockPos(5, 1, 3); var deep = new BlockPos(2, 1, 6); var master = new BlockPos(4, 2, 4);
		helper.setBlock(ore, family.ore); helper.setBlock(deep, family.deepslateOre);
		var readings = OreSurvey.survey(helper.getLevel(), helper.absolutePos(ore), helper.getLevel().getRandom(), 0, 1);
		helper.assertTrue(readings.stream().anyMatch(r -> r.icon().equals(Jugcraft.id("vanadium_iron_ore"))), "Prospector missed vanadium_iron");
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
			helper.assertTrue(normal == 1 && deepslate == 1, "Drill did not recover both finite vanadium_iron ore blocks");
		});
	}

	private static net.minecraft.world.item.Item item(String name) {
		return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Jugcraft.id(name));
	}
	private static MachineBlockEntity machine(GameTestHelper helper, BlockPos pos, MachineKind kind, ItemStack input, long energy) {
		helper.setBlock(pos, JugcraftMachines.MACHINES.get(kind));
		var machine = helper.getBlockEntity(pos, MachineBlockEntity.class);
		((SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP)).setAmount(energy);
		machine.setItem(0, input);
		return machine;
	}
	@GameTest(maxTicks = 600) public void crusherPaysForEveryInputAndNeverMultiplies(GameTestHelper helper) {
		var family = JugcraftMaterials.VANADIUM_IRON;
		var feeds = List.of(family.item, family.ore.asItem(), family.deepslateOre.asItem());
		var machines = new java.util.ArrayList<MachineBlockEntity>();
		var stores = new java.util.ArrayList<SimpleEnergyStorage>();
		for (int n = 0; n < feeds.size(); n++) {
			var pos = new BlockPos(1 + n * 3, 1, 1);
			machines.add(machine(helper, pos, MachineKind.CRUSHER, new ItemStack(feeds.get(n)), 3200));
			stores.add((SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP));
		}
		var fast = machine(helper, new BlockPos(1, 1, 5), MachineKind.CRUSHER, new ItemStack(family.item), 10000);
		fast.setItem(MachineKind.CRUSHER.slots, new ItemStack(MachineUpgrades.SPEED, 4));
		var stopped = machine(helper, new BlockPos(4, 1, 5), MachineKind.CRUSHER, new ItemStack(family.item), 0);
		var full = machine(helper, new BlockPos(7, 1, 5), MachineKind.CRUSHER, new ItemStack(family.item), 3200);
		full.setItem(MachineKind.CRUSHER.outputSlot(), new ItemStack(item("vanadium_iron_concentrate"), 64));
		helper.runAtTickTime(260, () -> {
			for (int n = 0; n < machines.size(); n++) {
				var output = machines.get(n).getItem(MachineKind.CRUSHER.outputSlot());
				helper.assertTrue(output.is(item("vanadium_iron_concentrate")) && output.getCount() == 1
						&& machines.get(n).getItem(0).isEmpty() && stores.get(n).getAmount() == 0, "One feed must pay 3200 JE for one concentrate");
			}
			helper.assertTrue(fast.getItem(MachineKind.CRUSHER.outputSlot()).getCount() == 1 && fast.getItem(0).isEmpty(), "Speed upgrade multiplied the ore");
			helper.assertTrue(stopped.getItem(0).getCount() == 1 && stopped.getItem(MachineKind.CRUSHER.outputSlot()).isEmpty(), "Unpowered crusher processed ore");
			helper.assertTrue(full.getItem(0).getCount() == 1 && full.getItem(MachineKind.CRUSHER.outputSlot()).getCount() == 64, "Full output lost or duplicated feed");
			full.setItem(MachineKind.CRUSHER.outputSlot(), ItemStack.EMPTY);
		});
		helper.runAtTickTime(520, () -> {
			helper.assertTrue(full.getItem(0).isEmpty() && full.getItem(MachineKind.CRUSHER.outputSlot()).getCount() == 1,
					"Crusher did not resume exactly once after freeing its output");
			helper.succeed();
		});
	}
	@GameTest(maxTicks = 400) public void waterAndPowerProduceTheAcidFreeContactBed(GameTestHelper helper) {
		var pos = new BlockPos(2, 1, 2);
		var washer = machine(helper, pos, MachineKind.ORE_WASHER, new ItemStack(item("vanadium_iron_concentrate")), 3200);
		var inlet = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			helper.assertTrue(inlet.insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), 500 * 81, transaction) == 500 * 81, "Washer refused water");
			transaction.commit();
		}
		var dry = machine(helper, new BlockPos(6, 1, 2), MachineKind.ORE_WASHER, new ItemStack(item("vanadium_iron_concentrate")), 3200);
		helper.runAtTickTime(260, () -> {
			var oxide = washer.getItem(MachineKind.ORE_WASHER.outputSlot());
			helper.assertTrue(oxide.is(item("vanadium_oxide")) && oxide.getCount() == 1 && washer.getItem(0).isEmpty(), "Washer did not produce one oxide");
			helper.assertTrue(((SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP)).getAmount() == 0, "Washer did not pay 3200 JE");
			try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
				helper.assertTrue(inlet.insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),
						MachineKind.WASHER_TANK * 81L, transaction) == MachineKind.WASHER_TANK * 81L, "Washer did not pay 500 mB water");
				// Roll back this capacity probe; the machine stays empty.
			}
			helper.assertTrue(dry.getItem(0).getCount() == 1 && dry.getItem(MachineKind.ORE_WASHER.outputSlot()).isEmpty(), "Dry washer processed concentrate");
			var input = CraftingInput.of(3, 1, List.of(new ItemStack(Items.BRICK), oxide.copy(), new ItemStack(Items.BRICK)));
			var bed = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value().assemble(input);
			helper.assertTrue(bed.is(item("vanadium_contact_bed")) && bed.getCount() == 1, "Oxide and two bricks did not make the existing contact bed");
			helper.succeed();
		});
	}
}
