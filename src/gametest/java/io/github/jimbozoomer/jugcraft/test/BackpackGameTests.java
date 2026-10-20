package io.github.jimbozoomer.jugcraft.test;

import com.tiviacz.travelersbackpack.blockentity.BackpackBlockEntity;
import com.tiviacz.travelersbackpack.component.Fluids;
import com.tiviacz.travelersbackpack.config.TravelersBackpackConfig;
import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.FluidVariantWrapper;
import eu.pb4.trinkets.api.TrinketsApi;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

/** Runs against the locked release, its real recipe serializers, and the Complete pack defaults. */
public class BackpackGameTests {
	private static boolean installed() {
		return FabricLoader.getInstance().isModLoaded("travelersbackpack");
	}

	static ItemStack stack(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id.contains(":") ? id : "travelersbackpack:" + id)));
	}

	private static ItemStack craft(GameTestHelper helper, String... slots) {
		List<ItemStack> grid = new ArrayList<>();
		for (String id : slots) grid.add(id.isEmpty() ? ItemStack.EMPTY : stack(id));
		while (grid.size() < 9) grid.add(ItemStack.EMPTY);
		var input = CraftingInput.of(3, 3, grid);
		return helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
				.map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
	}

	static ItemStack leather(GameTestHelper helper) {
		return craft(helper, "minecraft:leather", "minecraft:string", "minecraft:leather",
				"minecraft:leather", "minecraft:chest", "minecraft:leather", "minecraft:string", "blue_sleeping_bag", "");
	}

	@GameTest
	public void recipesUseIndustrialMaterialsAndRemoveBypasses(GameTestHelper helper) {
		if (!installed()) { helper.succeed(); return; }
		helper.assertTrue(leather(helper).is(stack("standard").getItem()), "The early leather backpack is craftable");
		String[][] recipes = {
			{"blank_upgrade", "jugcraft:bronze_plate", "jugcraft:bronze_plate", "minecraft:string", "minecraft:string", "minecraft:iron_nugget"},
			{"backpack_tank", "jugcraft:tin_plate", "jugcraft:tin_plate", "minecraft:glass", "minecraft:glass", "jugcraft:gasket"},
			{"hose_nozzle", "jugcraft:brass_plate", "jugcraft:brass_plate", "minecraft:glass", "minecraft:lever"},
			{"hose", "jugcraft:rubber", "jugcraft:rubber", "jugcraft:bronze_fluid_pipe", "jugcraft:gasket", "hose_nozzle"},
			{"iron_tier_upgrade", "blank_upgrade", "jugcraft:brass_plate", "jugcraft:brass_plate", "jugcraft:brass_plate", "jugcraft:brass_plate", "jugcraft:bronze_gear", "jugcraft:bronze_gear", "minecraft:redstone"},
			{"gold_tier_upgrade", "blank_upgrade", "jugcraft:steel_plate", "jugcraft:steel_plate", "jugcraft:steel_plate", "jugcraft:steel_plate", "jugcraft:invar_plate", "jugcraft:invar_plate", "jugcraft:advanced_circuit", "minecraft:gold_ingot"},
			{"diamond_tier_upgrade", "blank_upgrade", "jugcraft:aluminum_plate", "jugcraft:aluminum_plate", "jugcraft:plastic_sheet", "jugcraft:plastic_sheet", "jugcraft:gasket", "jugcraft:gasket", "jugcraft:advanced_circuit", "minecraft:diamond"},
			{"netherite_tier_upgrade", "blank_upgrade", "jugcraft:titanium_plate", "jugcraft:titanium_plate", "jugcraft:titanium_plate", "jugcraft:titanium_plate", "jugcraft:lithium_cell", "jugcraft:lithium_cell", "jugcraft:processor", "minecraft:netherite_ingot"},
			{"crafting_upgrade", "blank_upgrade", "minecraft:crafting_table", "jugcraft:bronze_plate", "jugcraft:bronze_plate"},
			{"lantern_upgrade", "blank_upgrade", "minecraft:lantern", "minecraft:glass", "jugcraft:copper_wire"},
			{"jukebox_upgrade", "blank_upgrade", "minecraft:note_block", "minecraft:diamond", "jugcraft:copper_wire", "minecraft:redstone"},
			{"pickup_upgrade", "blank_upgrade", "minecraft:hopper", "jugcraft:brass_plate", "jugcraft:brass_plate", "jugcraft:basic_circuit"},
			{"refill_upgrade", "blank_upgrade", "minecraft:dropper", "jugcraft:brass_plate", "jugcraft:brass_plate", "jugcraft:basic_circuit", "minecraft:redstone"},
			{"feeding_upgrade", "blank_upgrade", "minecraft:bowl", "minecraft:hopper", "jugcraft:brass_plate", "jugcraft:brass_plate", "jugcraft:basic_circuit"},
			{"furnace_upgrade", "blank_upgrade", "minecraft:furnace", "jugcraft:steel_plate", "jugcraft:steel_plate", "jugcraft:basic_circuit", "minecraft:brick", "minecraft:brick"},
			{"smoker_upgrade", "blank_upgrade", "minecraft:smoker", "jugcraft:steel_plate", "jugcraft:steel_plate", "jugcraft:basic_circuit", "minecraft:brick", "minecraft:brick"},
			{"tanks_upgrade", "blank_upgrade", "backpack_tank", "backpack_tank", "jugcraft:gasket", "jugcraft:gasket", "jugcraft:plastic_sheet"},
			{"magnet_upgrade", "blank_upgrade", "jugcraft:neodymium_magnet", "jugcraft:neodymium_magnet", "jugcraft:copper_wire", "jugcraft:copper_wire", "jugcraft:invar_plate", "jugcraft:invar_plate", "jugcraft:advanced_circuit"}
		};
		for (String[] recipe : recipes) {
			for (String id : recipe) helper.assertFalse(stack(id).isEmpty(), "Registered ingredient/output: " + id);
			ItemStack made = craft(helper, java.util.Arrays.copyOfRange(recipe, 1, recipe.length));
			helper.assertTrue(made.is(stack(recipe[0]).getItem()) && made.getCount() == 1, "Industrial recipe: " + recipe[0]);
		}
		for (var color : net.minecraft.world.item.DyeColor.values()) {
			String wool = "minecraft:" + color.getName() + "_wool";
			helper.assertTrue(craft(helper, wool, wool, wool).is(stack(color.getName() + "_sleeping_bag").getItem()), "Sleeping bag: " + color);
		}
		for (String removed : List.of("standard_no_sleeping_bag", "standard_no_tanks", "standard_no_tanks_sleeping_bag", "void_upgrade", "blast_furnace_upgrade")) {
			helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
					Identifier.fromNamespaceAndPath("travelersbackpack", removed))).isEmpty(), "Filtered upstream recipe: " + removed);
		}
		helper.succeed();
	}

	@GameTest
	public void sequentialSmithingPreservesContentsAndUsesApprovedCapacities(GameTestHelper helper) {
		if (!installed()) { helper.succeed(); return; }
		ItemStack bag = leather(helper);
		helper.assertTrue(bag.getOrDefault(ModDataComponents.SLEEPING_BAG_COLOR, -1) == net.minecraft.world.item.DyeColor.BLUE.getId(), "Crafting preserves sleeping bag color");
		var stored = ItemContainerContents.fromItems(List.of(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 17)));
		var tools = ItemContainerContents.fromItems(List.of(new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE)));
		var upgrades = ItemContainerContents.fromItems(List.of(stack("tanks_upgrade")));
		var fluids = new Fluids(new FluidVariantWrapper(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), FluidConstants.BUCKET), FluidVariantWrapper.blank());
		bag.set(DataComponents.CUSTOM_NAME, Component.literal("Expedition"));
		bag.set(ModDataComponents.BACKPACK_CONTAINER, stored);
		bag.set(ModDataComponents.TOOLS_CONTAINER, tools);
		bag.set(ModDataComponents.UPGRADES, upgrades);
		bag.set(ModDataComponents.FLUIDS, fluids);
		int[] storage = {27, 45, 63, 81, 99};
		int[] buckets = {2, 5, 7, 9, 11};
		String[] kits = {"iron", "gold", "diamond", "netherite"};
		for (int tier = 0; tier < 5; tier++) {
			var wrapper = BackpackWrapper.fromStack(bag);
			helper.assertTrue(wrapper.getStorageSize() == storage[tier] && wrapper.getToolSize() == tier + 2
					&& wrapper.getUpgradesSize() == tier + 2, "Approved slots at tier " + tier);
			helper.assertTrue(wrapper.getBackpackTankCapacity() == buckets[tier] * FluidConstants.BUCKET, "Real Fabric tank capacity at tier " + tier);
			helper.assertTrue(stored.equals(bag.get(ModDataComponents.BACKPACK_CONTAINER)) && tools.equals(bag.get(ModDataComponents.TOOLS_CONTAINER))
					&& upgrades.equals(bag.get(ModDataComponents.UPGRADES)) && fluids.equals(bag.get(ModDataComponents.FLUIDS))
					&& bag.getHoverName().getString().equals("Expedition"), "Upgrades preserve inventories, fluids and custom name at tier " + tier);
			if (tier < 4) {
				if (tier < 3) helper.assertTrue(smith(helper, bag, kits[tier + 1]).isEmpty(), "Cannot skip tier " + tier);
				bag = smith(helper, bag, kits[tier]);
				helper.assertFalse(bag.isEmpty(), "Sequential smithing succeeds at tier " + tier);
			}
		}
		helper.succeed();
	}

	private static ItemStack smith(GameTestHelper helper, ItemStack bag, String kit) {
		var input = new SmithingRecipeInput(stack("minecraft:leather"), bag, stack(kit + "_tier_upgrade"));
		return helper.getLevel().recipeAccess().getRecipeFor(RecipeType.SMITHING, input, helper.getLevel())
				.map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
	}

	@GameTest
	public void backSlotAndPlacedAutomationRemainAvailable(GameTestHelper helper) {
		if (!installed()) { helper.succeed(); return; }
		var settings = TravelersBackpackConfig.SERVER;
		helper.assertTrue(settings.backpackSettings.backSlotIntegration.get() && settings.backpackSettings.allowOnlyEquippedBackpack.get(), "Back slot and equipped-only access enabled");
		helper.assertFalse(settings.backpackAbilities.enableBackpackAbilities.get(), "No variant ability progression bypass");
		helper.assertFalse(TravelersBackpackConfig.COMMON.enableLoot.get() || TravelersBackpackConfig.COMMON.enableVillagerTrade.get(),
				"Backpack loot and villager acquisition are disabled at runtime");
		helper.assertFalse(settings.backpackUpgrades.enableBlastFurnaceUpgrade.get() || settings.backpackUpgrades.voidUpgradeSettings.enableUpgrade.get(), "Disallowed upgrades disabled");
		var player = helper.makeMockServerPlayerInLevel();
		var back = TrinketsApi.getAttachment(player).getInventory("chest/back");
		helper.assertTrue(back != null && back.getContainerSize() == 1, "One real Back accessory slot");
		ItemStack bag = leather(helper);
		helper.assertTrue(bag.is(TagKey.create(Registries.ITEM, Identifier.parse("trinkets:chest/back"))), "Backpack accepted by Back slot tag");
		back.setItem(0, bag);
		helper.assertTrue(back.getItem(0).is(bag.getItem()), "Back slot holds the backpack");
		back.setItem(0, ItemStack.EMPTY);
		bag.set(ModDataComponents.UPGRADES, ItemContainerContents.fromItems(List.of(stack("tanks_upgrade"))));
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, BuiltInRegistries.BLOCK.getValue(Identifier.parse("travelersbackpack:standard")));
		var block = helper.getBlockEntity(pos, BackpackBlockEntity.class);
		block.setBackpack(bag);
		var storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		helper.assertTrue(storage != null, "Placed backpack exposes Fabric item storage to logistics");
		try (var tx = Transaction.openOuter()) {
			helper.assertTrue(storage.insert(ItemVariant.of(net.minecraft.world.item.Items.IRON_INGOT), 12, tx) == 12, "Automation inserts items");
			tx.commit();
		}
		try (var tx = Transaction.openOuter()) {
			helper.assertTrue(storage.extract(ItemVariant.of(net.minecraft.world.item.Items.IRON_INGOT), 12, tx) == 12, "Automation extracts the same items");
			tx.commit();
		}
		var fluidStorage = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.EAST);
		helper.assertTrue(fluidStorage != null, "Placed tank exposes Fabric fluid storage to pipes");
		var water = FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER);
		try (var tx = Transaction.openOuter()) {
			helper.assertTrue(fluidStorage.insert(water, 3 * FluidConstants.BUCKET, tx) == 2 * FluidConstants.BUCKET,
					"A real Leather tank holds exactly two buckets");
			tx.commit();
		}
		try (var tx = Transaction.openOuter()) {
			helper.assertTrue(fluidStorage.extract(water, 3 * FluidConstants.BUCKET, tx) == 2 * FluidConstants.BUCKET,
					"Pipes recover exactly the stored water");
			tx.commit();
		}
		helper.succeed();
	}
}
