package io.github.jimbozoomer.jugcraft.arcane;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Native equivalent of the supplied component-sensitive strong-regeneration recipe. */
public final class ArcaneAurealRecipe extends CustomRecipe {
    private static final ArcaneAurealRecipe INSTANCE=new ArcaneAurealRecipe();
    public static final RecipeSerializer<ArcaneAurealRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,ArcaneAurealRecipe>unit(INSTANCE));
    @Override public boolean matches(CraftingInput in,Level level){
        if(in.width()!=3||in.height()!=3)return false;
        for(int i=0;i<9;i++){var s=in.getItem(i);if(i==4){var potion=s.get(DataComponents.POTION_CONTENTS);if(!s.is(Items.POTION)||potion==null||!potion.is(Potions.STRONG_REGENERATION))return false;}else if(!s.is(ForbiddenArcane.item("arcane_crystal_dust")))return false;}
        return true;
    }
    @Override public ItemStack assemble(CraftingInput input){return new ItemStack(ForbiddenArcane.item("aureal_bottle"));}
    @Override public RecipeSerializer<ArcaneAurealRecipe> getSerializer(){return SERIALIZER;}
}
