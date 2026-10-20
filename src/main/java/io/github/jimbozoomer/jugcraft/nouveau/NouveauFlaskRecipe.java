package io.github.jimbozoomer.jugcraft.nouveau;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
/** One real potion adds one compatible dose, preserving the flask's name and returning its bottle. */
public final class NouveauFlaskRecipe extends CustomRecipe {
    public static final NouveauFlaskRecipe INSTANCE=new NouveauFlaskRecipe();
    public static final RecipeSerializer<NouveauFlaskRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,NouveauFlaskRecipe>unit(INSTANCE));
    private record Fill(ItemStack flask,PotionContents potion,int bottle){}
    private static Fill fill(CraftingInput grid){ItemStack flask=ItemStack.EMPTY;PotionContents potion=null;int bottle=-1;
        for(int i=0;i<grid.size();i++){var s=grid.getItem(i);if(s.isEmpty())continue;if(s.is(Items.POTION)&&potion==null){potion=s.get(DataComponents.POTION_CONTENTS);bottle=i;}
            else if(s.getItem() instanceof NouveauPotions.Flask f&&!f.source().endsWith("cannon")&&flask.isEmpty())flask=s;else return null;}
        if(flask.isEmpty()||potion==null||flask.getOrDefault(NouveauPotions.DOSES,0)>=8||flask.getOrDefault(NouveauPotions.DOSES,0)>0&&!potion.equals(flask.get(DataComponents.POTION_CONTENTS)))return null;return new Fill(flask,potion,bottle);
    }
    public boolean matches(CraftingInput grid,Level l){return JugNouveau.enabled()&&fill(grid)!=null;}
    public ItemStack assemble(CraftingInput grid){var f=fill(grid);if(f==null)return ItemStack.EMPTY;var result=f.flask.copyWithCount(1);result.set(DataComponents.POTION_CONTENTS,f.potion);result.set(NouveauPotions.DOSES,result.getOrDefault(NouveauPotions.DOSES,0)+1);return result;}
    public NonNullList<ItemStack> getRemainingItems(CraftingInput grid){var left=NonNullList.withSize(grid.size(),ItemStack.EMPTY);var f=fill(grid);if(f!=null)left.set(f.bottle,new ItemStack(Items.GLASS_BOTTLE));return left;}
    public RecipeSerializer<NouveauFlaskRecipe> getSerializer(){return SERIALIZER;}
}
