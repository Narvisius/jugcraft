package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Exactly one labelled mat plus blank mats; only its label is copied, never arbitrary container data. */
public final class DoorMatCopyRecipe extends CustomRecipe {
    private static final DoorMatCopyRecipe INSTANCE=new DoorMatCopyRecipe();
    public static final RecipeSerializer<DoorMatCopyRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,DoorMatCopyRecipe>unit(INSTANCE));
    private record Copy(ItemStack source,int index,int blanks){}
    private Copy find(CraftingInput in){
        ItemStack source=ItemStack.EMPTY;int index=-1,blanks=0;
        for(int i=0;i<in.size();i++){var stack=in.getItem(i);if(stack.isEmpty())continue;if(!stack.is(Menagerie.ITEMS.get("household_door_mat")))return null;
            if(stack.has(DataComponents.CUSTOM_NAME)){if(!source.isEmpty())return null;source=stack;index=i;}else blanks++;
        }return source.isEmpty()||blanks==0?null:new Copy(source,index,blanks);
    }
    @Override public boolean matches(CraftingInput in,Level level){return find(in)!=null;}
    @Override public ItemStack assemble(CraftingInput in){var match=find(in);if(match==null)return ItemStack.EMPTY;var out=new ItemStack(Menagerie.item("household_door_mat"),match.blanks);out.set(DataComponents.CUSTOM_NAME,match.source.get(DataComponents.CUSTOM_NAME));return out;}
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput in){var left=NonNullList.withSize(in.size(),ItemStack.EMPTY);var match=find(in);if(match!=null)left.set(match.index,match.source.copyWithCount(1));return left;}
    @Override public RecipeSerializer<DoorMatCopyRecipe> getSerializer(){return SERIALIZER;}
}
