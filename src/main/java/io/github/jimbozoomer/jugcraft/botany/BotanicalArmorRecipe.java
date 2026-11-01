package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Component-preserving ink toggle and ancient-will attachment; no duplicate outputs. */
public final class BotanicalArmorRecipe extends CustomRecipe {
    private static final BotanicalArmorRecipe INSTANCE=new BotanicalArmorRecipe();
    public static final RecipeSerializer<BotanicalArmorRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,BotanicalArmorRecipe>unit(INSTANCE));
    private static final List<String> NAMES=List.of("ahrim","dharok","guthan","torag","verac","karil");
    private ItemStack result(CraftingInput in){
        ItemStack armor=ItemStack.EMPTY;boolean ink=false;int wills=0;
        for(int i=0;i<in.size();i++){
            var s=in.getItem(i);if(s.isEmpty())continue;
            if(s.getItem() instanceof BotanicalArmor.Armor||BotanicalCosmetics.attachable(s)){if(!armor.isEmpty())return ItemStack.EMPTY;armor=s;continue;}
            if(s.is(Jugtania.item("phantom_ink"))){if(ink)return ItemStack.EMPTY;ink=true;continue;}
            String id=BuiltInRegistries.ITEM.getKey(s.getItem()).toString();int bit=-1;
            for(int n=0;n<NAMES.size();n++)if(id.equals("jugcraft:jugtania_ancient_will_"+NAMES.get(n)))bit=n;
            if(bit<0||(wills&(1<<bit))!=0)return ItemStack.EMPTY;wills|=1<<bit;
        }
        if(armor.isEmpty()||ink==(wills!=0))return ItemStack.EMPTY;
        var result=armor.copyWithCount(1);
        if(ink){result.set(BotanicalArmor.PHANTOM,!BotanicalArmor.invisible(armor));return result;}
        if(!armor.is(Jugtania.item("terrasteel_helmet")))return ItemStack.EMPTY;
        int old=armor.getOrDefault(BotanicalArmor.WILLS,0);if((old&wills)!=0)return ItemStack.EMPTY;result.set(BotanicalArmor.WILLS,old|wills);return result;
    }
    @Override public boolean matches(CraftingInput input,Level level){return !result(input).isEmpty();}
    @Override public ItemStack assemble(CraftingInput input){return result(input);}
    @Override public RecipeSerializer<BotanicalArmorRecipe> getSerializer(){return SERIALIZER;}
}
