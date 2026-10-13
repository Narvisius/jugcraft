package local.peepo;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Eight iron ingots around the wooden crate; preserves filled contents, names and other components. */
public final class TransportCrateUpgrade extends CustomRecipe {
    public static final TransportCrateUpgrade INSTANCE=new TransportCrateUpgrade();
    public static final RecipeSerializer<TransportCrateUpgrade> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,TransportCrateUpgrade>unit(INSTANCE));
    @Override public boolean matches(CraftingInput input,Level level){
        if(input.width()!=3 || input.height()!=3)return false;
        for(int i=0;i<9;i++)if(!input.getItem(i).is(i==4?MobTransportCrate.WOOD:Items.IRON_INGOT))return false;
        return true;
    }
    @Override public ItemStack assemble(CraftingInput input){return matches(input,null)?input.getItem(4).transmuteCopy(MobTransportCrate.IRON,1):ItemStack.EMPTY;}
    @Override public RecipeSerializer<TransportCrateUpgrade> getSerializer(){return SERIALIZER;}
}
