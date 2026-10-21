package io.github.jimbozoomer.jugcraft.workshop;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class JugcraftWorkshop {
    public static final String[] STATIONS = {"workshop_bench", "small_ceramic_kiln", "small_crucible_furnace"};
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final WorkshopBlock[] BLOCKS = new WorkshopBlock[3];
    public static BlockEntityType<WorkshopBlockEntity> ENTITY;
    public static ExtendedMenuType<WorkshopMenu, WorkshopCatalog> MENU;
    public static RecipeType<WorkshopRecipe> RECIPES;
    public static RecipeSerializer<WorkshopRecipe> SERIALIZER;
    private JugcraftWorkshop() { }
    public static void register() {
        for (String name : new String[]{"workshop_hammer", "wire_cutters", "soldering_tool", "unfired_ceramic_lining",
                "basic_ceramic_lining", "unfired_ceramic_crucible", "ceramic_crucible", "unfired_insulator"}) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
            var properties = new Item.Properties().setId(key);
            if (name.equals("workshop_hammer") || name.equals("wire_cutters") || name.equals("soldering_tool")) properties.stacksTo(1);
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new Item(properties)));
        }
        for (int i = 0; i < STATIONS.length; i++) {
            String name = STATIONS[i];
            var bk = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
            BLOCKS[i] = Registry.register(BuiltInRegistries.BLOCK, bk, new WorkshopBlock(
                BlockBehaviour.Properties.ofFullCopy(i == 0 ? Blocks.CRAFTING_TABLE : Blocks.BRICKS).setId(bk)
                    .strength(2.5F).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE), i));
            var ik = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(BLOCKS[i], new Item.Properties().setId(ik).useBlockDescriptionPrefix())));
        }
        ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("starter_workshop"),
            FabricBlockEntityTypeBuilder.create(WorkshopBlockEntity::new, BLOCKS).build());
        MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("starter_workshop"),
            new ExtendedMenuType<>(WorkshopMenu::new, WorkshopCatalog.STREAM_CODEC));
        RECIPES = Registry.register(BuiltInRegistries.RECIPE_TYPE, Jugcraft.id("starter_working"), new RecipeType<>() {
            public String toString() { return "jugcraft:starter_working"; }
        });
        SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id("starter_working"),
            new RecipeSerializer<>(WorkshopRecipe.CODEC, WorkshopRecipe.STREAM_CODEC));
        WorkshopCatalog.register();
        WorkshopBlock.registerProtection();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> ITEMS.values().forEach(out::accept));
    }
}
