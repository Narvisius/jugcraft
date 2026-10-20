package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Component-preserving repair and insulation. One reagent and one real piece of equipment. */
public final class BeastUpgradeRecipe extends CustomRecipe {
    private static final BeastUpgradeRecipe REPAIR=new BeastUpgradeRecipe(false),FUR=new BeastUpgradeRecipe(true);
    public static final RecipeSerializer<BeastUpgradeRecipe> REPAIR_SERIALIZER=new RecipeSerializer<>(MapCodec.unit(REPAIR),StreamCodec.<RegistryFriendlyByteBuf,BeastUpgradeRecipe>unit(REPAIR));
    public static final RecipeSerializer<BeastUpgradeRecipe> FUR_SERIALIZER=new RecipeSerializer<>(MapCodec.unit(FUR),StreamCodec.<RegistryFriendlyByteBuf,BeastUpgradeRecipe>unit(FUR));
    private final boolean fur;
    private BeastUpgradeRecipe(boolean fur){this.fur=fur;}
    private ItemStack equipment(CraftingInput in){
        ItemStack gear=ItemStack.EMPTY;boolean reagent=false;
        for(int i=0;i<in.size();i++){var stack=in.getItem(i);if(stack.isEmpty())continue;
            if(stack.is(Menagerie.ITEMS.get(fur?"beast_bison_fur":"beast_mimicream"))&&!reagent)reagent=true;
            else if(gear.isEmpty()&&(fur?stack.has(DataComponents.EQUIPPABLE)&&!stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("BisonInsulated",false):stack.isDamaged()))gear=stack;else return ItemStack.EMPTY;
        }return reagent?gear:ItemStack.EMPTY;
    }
    @Override public boolean matches(CraftingInput in,Level l){return !equipment(in).isEmpty();}
    @Override public ItemStack assemble(CraftingInput in){var gear=equipment(in);if(gear.isEmpty())return ItemStack.EMPTY;var result=gear.copyWithCount(1);if(fur){var tag=result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putBoolean("BisonInsulated",true);result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}else result.setDamageValue(Math.max(0,result.getDamageValue()-Math.max(1,result.getMaxDamage()/4)));return result;}
    @Override public RecipeSerializer<BeastUpgradeRecipe> getSerializer(){return fur?FUR_SERIALIZER:REPAIR_SERIALIZER;}
}
