package io.github.jimbozoomer.jugcraft.aquatic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** One fish and one knife. The knife returns with wear; no duplicate-tool remainders. */
public final class OwnerFilletRecipe extends CustomRecipe {
    public static final OwnerFilletRecipe INSTANCE=new OwnerFilletRecipe();
    public static final RecipeSerializer<OwnerFilletRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.unit(INSTANCE));
    private int fish(CraftingInput input){
        int fish=-1,knife=-1;
        for(int i=0;i<input.size();i++){
            var stack=input.getItem(i);if(stack.isEmpty())continue;
            if(OwnerAquaculture.fillets(stack)>0){if(fish>=0)return -1;fish=i;}
            else if(stack.getItem() instanceof OwnerAquaticGear.Knife){if(knife>=0)return -1;knife=i;}
            else return -1;
        }
        return knife>=0?fish:-1;
    }
    @Override public boolean matches(CraftingInput input,Level level){return fish(input)>=0;}
    @Override public ItemStack assemble(CraftingInput input){
        int index=fish(input);if(index<0)return ItemStack.EMPTY;
        int amount=OwnerFishWeights.fillets(input.getItem(index),OwnerAquaculture.fillets(input.getItem(index)));
        for(int i=0;i<input.size();i++)if(input.getItem(i).is(OwnerAquaculture.ITEMS.get("neptunium_fillet_knife")))amount+=amount/4;
        return new ItemStack(OwnerAquaculture.ITEMS.get("fish_fillet_raw"),amount);
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){
        var result=NonNullList.withSize(input.size(),ItemStack.EMPTY);if(fish(input)<0)return result;
        for(int i=0;i<input.size();i++)if(input.getItem(i).getItem() instanceof OwnerAquaticGear.Knife knife){
            var stack=input.getItem(i).copyWithCount(1);
            if(!knife.eternal){stack.setDamageValue(stack.getDamageValue()+1);if(stack.getDamageValue()>=stack.getMaxDamage())stack=ItemStack.EMPTY;}
            result.set(i,stack);
        }
        return result;
    }
    @Override public RecipeSerializer<OwnerFilletRecipe> getSerializer(){return SERIALIZER;}
}
