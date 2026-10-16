package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Pine wood's approval preview: normal building blocks, with no pine tree or world generation yet. */
public class PineWoodGameTests {
	private static final List<String> LOGS = List.of("pine_log", "pine_wood", "stripped_pine_log", "stripped_pine_wood");
	private static final List<String> BLOCKS = List.of("pine_log", "pine_wood", "stripped_pine_log", "stripped_pine_wood",
			"pine_planks", "pine_stairs", "pine_slab", "pine_fence", "pine_fence_gate");
	private static final BlockPos AT = new BlockPos(2, 2, 2);

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	/** Both barked forms strip once, preserving every placement axis and charging one axe durability. */
	@GameTest
	public void axesStripLogsAndWoodAlongEveryAxis(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		for (String form : List.of("pine_log", "pine_wood")) {
			for (Direction.Axis axis : Direction.Axis.values()) {
				BlockState original = block(form).defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
				helper.setBlock(AT, original);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
				helper.useBlock(AT, player);
				helper.assertTrue(helper.getBlockState(AT).equals(original), "A stick changed " + form);
				ItemStack axe = new ItemStack(Items.IRON_AXE);
				player.setItemInHand(InteractionHand.MAIN_HAND, axe);
				helper.useBlock(AT, player);
				BlockState stripped = helper.getBlockState(AT);
				helper.assertTrue(stripped.is(block("stripped_" + form)) && stripped.getValue(RotatedPillarBlock.AXIS) == axis,
						"Stripping changed the axis or produced the wrong block: " + stripped);
				helper.assertTrue(axe.getDamageValue() == 1, "Stripping must consume one axe durability");
				helper.useBlock(AT, player);
				helper.assertTrue(helper.getBlockState(AT).equals(stripped) && axe.getDamageValue() == 1,
						"An already stripped block changed or consumed another axe use");
			}
		}
		helper.succeed();
	}

	/** Real recipe matching covers all log inputs, every building shape and vanilla planks consumers. */
	@GameTest
	public void pineCraftsIntoBuildingBlocksAndVanillaItems(GameTestHelper helper) {
		for (String form : LOGS) {
			craft(helper, item("pine_planks"), 4, 1, 1, item(form));
		}
		Item log = item("pine_log");
		Item stripped = item("stripped_pine_log");
		Item plank = item("pine_planks");
		craft(helper, item("pine_wood"), 3, 2, 2, log, log, log, log);
		craft(helper, item("stripped_pine_wood"), 3, 2, 2, stripped, stripped, stripped, stripped);
		craft(helper, item("pine_stairs"), 4, 3, 3, plank, null, null, plank, plank, null, plank, plank, plank);
		craft(helper, item("pine_slab"), 6, 3, 1, plank, plank, plank);
		craft(helper, item("pine_fence"), 3, 3, 2, plank, Items.STICK, plank, plank, Items.STICK, plank);
		craft(helper, item("pine_fence_gate"), 1, 3, 2, Items.STICK, plank, Items.STICK, Items.STICK, plank, Items.STICK);
		craft(helper, Items.STICK, 4, 1, 2, plank, plank);
		craft(helper, Items.CRAFTING_TABLE, 1, 2, 2, plank, plank, plank, plank);
		helper.succeed();
	}

	static void craft(GameTestHelper helper, Item expected, int count, int width, int height, Item... grid) {
		List<ItemStack> stacks = new ArrayList<>();
		for (Item ingredient : grid) {
			stacks.add(ingredient == null ? ItemStack.EMPTY : new ItemStack(ingredient));
		}
		CraftingInput input = CraftingInput.of(width, height, stacks);
		ItemStack result = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
				.map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
		helper.assertTrue(result.is(expected) && result.getCount() == count,
				"Expected " + count + " " + expected + " from " + stacks + ", got " + result);
	}

	/** Dropping placed wood preserves it; a double slab returns two, and fuel/vanilla wood tags are present. */
	@GameTest
	public void pineDropsAndTagsWorkAsWood(GameTestHelper helper) {
		TagKey<Block> burns = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("logs_that_burn"));
		for (String name : BLOCKS) {
			BlockState state = block(name).defaultBlockState();
			assertDrop(helper, state, item(name), 1);
			helper.assertTrue(new ItemStack(item(name)).has(DataComponents.COOKING_FUEL), name + " is not furnace fuel");
			if (LOGS.contains(name)) {
				helper.assertTrue(state.is(BlockTags.LOGS) && state.is(burns), name + " lacks burnable log tags");
			}
		}
		assertDrop(helper, block("pine_slab").defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), item("pine_slab"), 2);
		helper.assertTrue(new ItemStack(item("pine_planks")).is(ItemTags.PLANKS), "Pine planks lack the planks tag");
		helper.assertTrue(new ItemStack(item("pine_stairs")).is(ItemTags.WOODEN_STAIRS), "Pine stairs lack the wooden stairs tag");
		helper.assertTrue(new ItemStack(item("pine_slab")).is(ItemTags.WOODEN_SLABS), "Pine slabs lack the wooden slabs tag");
		helper.assertTrue(new ItemStack(item("pine_fence")).is(ItemTags.WOODEN_FENCES), "Pine fences lack the wooden fences tag");
		helper.assertTrue(block("pine_fence_gate").defaultBlockState().is(BlockTags.FENCE_GATES), "Pine gates lack the fence gate tag");
		helper.succeed();
	}

	private static void assertDrop(GameTestHelper helper, BlockState state, Item expected, int count) {
		List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(AT), null);
		helper.assertTrue(drops.size() == 1 && drops.getFirst().is(expected) && drops.getFirst().getCount() == count,
				state + " returned " + drops + " instead of " + count + " " + expected);
	}

	/** Every log form processes through the real sawmill, consuming its input and the normal power bill. */
	@GameTest
	public void sawmillProcessesEveryPineLogForm(GameTestHelper helper) {
		helper.setBlock(AT, JugcraftMachines.MACHINES.get(MachineKind.SAWMILL));
		MachineBlockEntity machine = helper.getBlockEntity(AT, MachineBlockEntity.class);
		SimpleEnergyStorage energy = (SimpleEnergyStorage) machine.energyFor(null);
		for (String form : LOGS) {
			var recipe = MachineRecipes.find(helper.getLevel(), MachineKind.SAWMILL, new ItemStack(item(form)))
					.orElseThrow(() -> helper.assertionException("No sawmill recipe accepts " + form));
			helper.assertTrue(recipe.time() == 100, "Pine should use the normal 100-tick sawmill recipe");
			for (int slot = 0; slot < machine.getContainerSize(); slot++) {
				machine.setItem(slot, ItemStack.EMPTY);
			}
			machine.setItem(0, new ItemStack(item(form)));
			energy.setAmount(0);
			machine.serverTick(helper.getLevel(), helper.absolutePos(AT), helper.getBlockState(AT));
			helper.assertTrue(machine.getItem(0).getCount() == 1 && machine.getItem(MachineKind.SAWMILL.outputSlot()).isEmpty(),
					"The sawmill processed " + form + " without power");
			long charge = energy.getCapacity();
			energy.setAmount(charge);
			for (int tick = 0; tick < recipe.time(); tick++) {
				machine.serverTick(helper.getLevel(), helper.absolutePos(AT), helper.getBlockState(AT));
			}
			ItemStack output = machine.getItem(MachineKind.SAWMILL.outputSlot());
			helper.assertTrue(output.is(item("pine_planks")) && output.getCount() == 6 && machine.getItem(0).isEmpty(),
					"The sawmill did not turn one " + form + " into six pine planks: " + output);
			helper.assertTrue(charge - energy.getAmount() == (long) recipe.time() * MachineKind.SAWMILL.usePerTick,
					"The sawmill did not pay its normal energy bill for " + form);
		}
		helper.succeed();
	}
}
