package io.github.jimbozoomer.jugcraft.concordance.assay;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;

/** Alchemical crafting progression and durable tools over the existing personal essence ledger. */
public final class DarkMatter {
    public static final int DURABILITY = 4096;
    public static final int[] BURN_TICKS = {6400, 12800, 25600};
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final ToolMaterial MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            DURABILITY, 9.0F, 4.0F, 10, TagKey.create(Registries.ITEM, Jugcraft.id("repairs_dark_matter_tools")));
    public static DataComponentType<Integer> MODE;
    private DarkMatter() { }

    public static void register() {
        MODE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("dark_matter_mode"),
                DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 2)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        for (String id : new String[]{"low_covalence_dust", "medium_covalence_dust", "high_covalence_dust", "dark_matter"}) material(id, 0);
        String[] fuels = {"alchemical_coal", "mobius_fuel", "aeternalis_fuel"};
        for (int i = 0; i < fuels.length; i++) material(fuels[i], BURN_TICKS[i]);
        for (DarkMatterTool.Kind kind : DarkMatterTool.Kind.values()) {
            String id = "dark_matter_" + kind.id;
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
            var properties = new Item.Properties().setId(key).rarity(Rarity.RARE).component(MODE, 0);
            if (kind == DarkMatterTool.Kind.AXE) properties.axe(MATERIAL, 5.0F, -3.1F);
            else properties.pickaxe(MATERIAL, kind == DarkMatterTool.Kind.HAMMER ? 3.0F : 1.0F, -2.8F);
            ITEMS.put(id, Registry.register(BuiltInRegistries.ITEM, key, new DarkMatterTool(properties, kind)));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> ITEMS.values().forEach(entries::accept));
    }

    private static void material(String id, int burnTicks) {
        var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
        var properties = new Item.Properties().setId(key).rarity(Rarity.UNCOMMON);
        if (burnTicks > 0) properties.component(DataComponents.COOKING_FUEL,
                new CookingFuel(new ResolvableInt.Constant(burnTicks), new ResolvableFloat.Constant(1.0F)));
        ITEMS.put(id, Registry.register(BuiltInRegistries.ITEM, key, new Item(properties) {
            @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                    Consumer<Component> text, TooltipFlag flag) {
                text.accept(Component.translatable("tooltip.jugcraft." + id).withStyle(ChatFormatting.GRAY));
            }
        }));
    }
}
