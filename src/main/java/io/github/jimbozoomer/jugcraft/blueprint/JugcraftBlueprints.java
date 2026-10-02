package io.github.jimbozoomer.jugcraft.blueprint;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The Blueprint System (docs/features/blueprint-system.md): the Blueprint Table, the Blueprint item, the
 * Survey Stake and the blueprint library. Placed blueprints are build jobs for drone depots.
 */
public final class JugcraftBlueprints {
	public static Block STAKE;
	public static BlockEntityType<SurveyStakeBlockEntity> STAKE_ENTITY;
	public static Item BLUEPRINT;
	public static Block TABLE;

	private JugcraftBlueprints() {
	}

	public static void register() {
		ResourceKey<Block> stakeKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("survey_stake"));
		STAKE = Registry.register(BuiltInRegistries.BLOCK, stakeKey, new SurveyStakeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)
				.setId(stakeKey).strength(0.5F).noOcclusion().noLootTable()));
		STAKE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("survey_stake"),
				FabricBlockEntityTypeBuilder.create(SurveyStakeBlockEntity::new, STAKE).build());
		// The stake has an item only so the block is complete; blueprints place it (it is not in any creative tab).
		ResourceKey<Item> stakeItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("survey_stake"));
		Registry.register(BuiltInRegistries.ITEM, stakeItemKey, new net.minecraft.world.item.BlockItem(STAKE,
				new Item.Properties().setId(stakeItemKey).useBlockDescriptionPrefix()));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("blueprint"));
		BLUEPRINT = Registry.register(BuiltInRegistries.ITEM, itemKey, new BlueprintItem(new Item.Properties().setId(itemKey).stacksTo(16)));
		ResourceKey<Block> tableKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("blueprint_table"));
		TABLE = Registry.register(BuiltInRegistries.BLOCK, tableKey, new BlueprintTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
				.setId(tableKey).noOcclusion()));
		ResourceKey<Item> tableItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("blueprint_table"));
		Registry.register(BuiltInRegistries.ITEM, tableItemKey, new net.minecraft.world.item.BlockItem(TABLE,
				new Item.Properties().setId(tableItemKey).useBlockDescriptionPrefix()));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(TABLE));
		BlueprintNetwork.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			for (String id : Blueprint.BUILT_IN) {
				output.accept(BlueprintItem.stack(id));
			}
		});
	}
}
