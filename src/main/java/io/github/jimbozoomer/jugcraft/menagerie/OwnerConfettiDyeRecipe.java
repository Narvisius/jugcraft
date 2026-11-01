package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** One popper and one to eight dyes; components retained, no duplicating stack counts. */
public final class OwnerConfettiDyeRecipe extends CustomRecipe {
    private static final OwnerConfettiDyeRecipe INSTANCE=new OwnerConfettiDyeRecipe();
    public static final RecipeSerializer<OwnerConfettiDyeRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,OwnerConfettiDyeRecipe>unit(INSTANCE));
    private record Match(ItemStack popper,List<Integer> colors){}
    private Match find(CraftingInput input){
        ItemStack popper=ItemStack.EMPTY;var colors=new ArrayList<Integer>();
        for(int i=0;i<input.size();i++){var s=input.getItem(i);if(s.isEmpty())continue;
            if(s.getItem() instanceof OwnerConfetti&&popper.isEmpty())popper=s;
            else if(s.getItem() instanceof DyeItem dye&&colors.size()<8)colors.add(0xFF000000|dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getTextureDiffuseColor());
            else return null;
        }return popper.isEmpty()||colors.isEmpty()?null:new Match(popper,List.copyOf(colors));
    }
    @Override public boolean matches(CraftingInput input,Level level){return find(input)!=null;}
    @Override public ItemStack assemble(CraftingInput input){var match=find(input);if(match==null)return ItemStack.EMPTY;var result=match.popper.copyWithCount(1);CustomData.update(DataComponents.CUSTOM_DATA,result,tag->tag.store("SuppsConfettiColors",Codec.INT.listOf(),match.colors));return result;}
    @Override public RecipeSerializer<OwnerConfettiDyeRecipe> getSerializer(){return SERIALIZER;}
}
