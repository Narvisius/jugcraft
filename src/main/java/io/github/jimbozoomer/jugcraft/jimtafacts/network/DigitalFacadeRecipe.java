package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Source anchor+block facade and ingot+knife recipes, using actual ingredients and knife wear. */
public final class DigitalFacadeRecipe extends CustomRecipe {
    private static final DigitalFacadeRecipe COVER = new DigitalFacadeRecipe(false), CUT = new DigitalFacadeRecipe(true);
    public static final RecipeSerializer<DigitalFacadeRecipe> FACADE = serializer(COVER), ANCHORS = serializer(CUT);
    private final boolean cut;
    private DigitalFacadeRecipe(boolean cut) { this.cut = cut; }
    private static RecipeSerializer<DigitalFacadeRecipe> serializer(DigitalFacadeRecipe recipe) { return new RecipeSerializer<>(MapCodec.unit(recipe), StreamCodec.<RegistryFriendlyByteBuf, DigitalFacadeRecipe>unit(recipe)); }
    private boolean valid(CraftingInput in) {
        if (cut) {
            int knives = 0, ingots = 0;
            for (var stack : in.items()) { if (stack.isEmpty()) continue; if (stack.getItem() instanceof DigitalEquipment.Knife) knives++; else if (DigitalPressMenu.ingot(stack)) ingots++; else return false; }
            return knives == 1 && ingots == 1;
        }
        if (in.width() != 3 || in.height() != 3 || !(in.getItem(4).getItem() instanceof BlockItem block) || !DigitalFacades.valid(block.getBlock().defaultBlockState())) return false;
        for (int i = 0; i < 9; i++) if (i != 4 && ((i & 1) == 1 ? !in.getItem(i).is(DigitalStorage.ITEMS.get("cable_anchor")) : !in.getItem(i).isEmpty())) return false;
        return true;
    }
    @Override public boolean matches(CraftingInput in, Level level) { return valid(in); }
    @Override public ItemStack assemble(CraftingInput in) { if (!valid(in)) return ItemStack.EMPTY; return cut ? new ItemStack(DigitalStorage.ITEMS.get("cable_anchor"), 4) : DigitalFacades.facade(((BlockItem) in.getItem(4).getItem()).getBlock().defaultBlockState()).copyWithCount(4); }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput in) {
        var remainder = NonNullList.withSize(in.size(), ItemStack.EMPTY);
        if (cut && valid(in)) for (int i = 0; i < in.size(); i++) if (in.getItem(i).getItem() instanceof DigitalEquipment.Knife) {
            var tool = in.getItem(i).copyWithCount(1); tool.setDamageValue(tool.getDamageValue() + 1); if (tool.getDamageValue() < tool.getMaxDamage()) remainder.set(i, tool);
        }
        return remainder;
    }
    @Override public RecipeSerializer<DigitalFacadeRecipe> getSerializer() { return cut ? ANCHORS : FACADE; }
}
