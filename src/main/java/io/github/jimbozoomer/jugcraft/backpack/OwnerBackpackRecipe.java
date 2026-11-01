package io.github.jimbozoomer.jugcraft.backpack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Retain the real bag/upgrade components while paying the supplied shaped recipe. */
public final class OwnerBackpackRecipe extends CustomRecipe {
    private final ShapedRecipe shaped;
    public static final RecipeSerializer<OwnerBackpackRecipe> SERIALIZER=new RecipeSerializer<>(ShapedRecipe.MAP_CODEC.xmap(OwnerBackpackRecipe::new,r->r.shaped),ShapedRecipe.STREAM_CODEC.map(OwnerBackpackRecipe::new,r->r.shaped));
    public OwnerBackpackRecipe(ShapedRecipe shaped){this.shaped=shaped;}
    @Override public boolean matches(CraftingInput input,Level level){return shaped.matches(input,level);}
    @Override public ItemStack assemble(CraftingInput input){
        ItemStack result=shaped.assemble(input);
        for(int i=0;i<input.size();i++){
            var source=input.getItem(i);
            if(source.getItem() instanceof OwnerBackpacks.PackItem||source.getItem() instanceof OwnerBackpacks.UpgradeItem upgrade&&OwnerBackpacks.UPGRADE_NAMES.contains(upgrade.name))return source.transmuteCopy(result.getItem(),result.getCount());
        }
        return result;
    }
    @Override public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display(){return shaped.display();}
    @Override public RecipeSerializer<OwnerBackpackRecipe> getSerializer(){return SERIALIZER;}
}
