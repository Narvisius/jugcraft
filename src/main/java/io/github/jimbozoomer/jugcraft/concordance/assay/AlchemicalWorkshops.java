package io.github.jimbozoomer.jugcraft.concordance.assay;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Five workshop blocks over the same audited essence catalogue and Klein Star component. */
public final class AlchemicalWorkshops {
    public static final Map<String, AlchemicalWorkshopBlock> BLOCKS = new LinkedHashMap<>();
    public static final int[] COLLECTOR_CAPACITY = {4096, 16384, 65536};
    public static final int[] COLLECTOR_RATE = {1, 4, 16};
    public static final int[] CONDENSER_CAPACITY = {1048576, 16777216};
    public static DataComponentType<Integer> ESSENCE;
    public static BlockEntityType<AlchemicalWorkshopBlockEntity> ENTITY;
    public static MenuType<AlchemicalWorkshopMenu> COLLECTOR_MENU, CONDENSER_MENU;

    private AlchemicalWorkshops() { }
    public static void register() {
        ESSENCE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("workshop_essence"),
                DataComponentType.<Integer>builder().persistent(Codec.intRange(0, CONDENSER_CAPACITY[1])).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        COLLECTOR_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("essence_collector"),
                new MenuType<>((id, inv) -> new AlchemicalWorkshopMenu(id, inv, true), FeatureFlags.DEFAULT_FLAGS));
        CONDENSER_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("essence_condenser"),
                new MenuType<>((id, inv) -> new AlchemicalWorkshopMenu(id, inv, false), FeatureFlags.DEFAULT_FLAGS));
        for (boolean collector : new boolean[]{true, false}) for (int tier = 1; tier <= (collector ? 3 : 2); tier++) {
            String name = (collector ? "collector_mk" : "condenser_mk") + tier;
            var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
            var block = new AlchemicalWorkshopBlock(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5F, 8).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> s.getValue(AlchemicalWorkshopBlock.ACTIVE) ? 5 : 0), collector, tier);
            Registry.register(BuiltInRegistries.BLOCK, key, block); BLOCKS.put(name, block);
            var itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
            Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block,
                    new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().stacksTo(1).component(ESSENCE, 0)) {
                @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                        Consumer<Component> text, TooltipFlag flag) {
                    text.accept(Component.translatable("tooltip.jugcraft." + name).withStyle(ChatFormatting.GRAY));
                    text.accept(Component.translatable("tooltip.jugcraft.workshop.stored", stack.getOrDefault(ESSENCE, 0)).withStyle(ChatFormatting.AQUA));
                }
            });
        }
        ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("alchemical_workshop"),
                FabricBlockEntityTypeBuilder.create(AlchemicalWorkshopBlockEntity::new, BLOCKS.values().toArray(Block[]::new)).build());
        // Unsided automation must not bypass the manual-only sample/star slots.
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((be, side) -> side == null
                ? net.fabricmc.fabric.api.transfer.v1.storage.Storage.empty()
                : net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, side), ENTITY);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> BLOCKS.values().forEach(e::accept));
    }
}
