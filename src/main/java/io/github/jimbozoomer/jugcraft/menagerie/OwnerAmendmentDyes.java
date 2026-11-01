package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** Stored dye colors, bounded recoloring and recipes that preserve the supplied item's components. */
public final class OwnerAmendmentDyes extends Item {
    public OwnerAmendmentDyes(Properties props){super(props.stacksTo(16).craftRemainder(Items.GLASS_BOTTLE).component(DataComponents.DYED_COLOR,new DyedItemColor(0xFFFFFF)));}
    public static int color(ItemStack s){return s.getOrDefault(DataComponents.DYED_COLOR,new DyedItemColor(0xFFFFFF)).rgb();}
    public static ItemStack bottle(int color){var s=new ItemStack(OwnerAmendments.DYE_BOTTLE);s.set(DataComponents.DYED_COLOR,new DyedItemColor(color&0xFFFFFF));return s;}
    // Mix in linear RGB rather than averaging gamma-encoded bytes; every poured portion has weight.
    public static int mix(int a,int b,int aw,int bw){int out=0;for(int shift:new int[]{16,8,0}){double x=Math.pow(((a>>shift)&255)/255.0,2.2),y=Math.pow(((b>>shift)&255)/255.0,2.2);int c=(int)Math.round(Math.pow((x*aw+y*bw)/(aw+bw),1/2.2)*255);out|=Math.clamp(c,0,255)<<shift;}return out;}
    public static DyeColor closest(int value){DyeColor best=DyeColor.WHITE;long distance=Long.MAX_VALUE;for(var dye:DyeColor.values()){int c=dye.getTextureDiffuseColor();long d=0;for(int shift:new int[]{16,8,0}){int delta=((value>>shift)&255)-((c>>shift)&255);d+=(long)delta*delta;}if(d<distance){distance=d;best=dye;}}return best;}
    public static ItemStack recolor(ItemStack stack,int color){
        if(stack.is(ItemTags.CAULDRON_CAN_REMOVE_DYE)){var result=stack.copyWithCount(1);if(stack.has(DataComponents.DYED_COLOR))color=mix(color,stack.get(DataComponents.DYED_COLOR).rgb(),1,1);result.set(DataComponents.DYED_COLOR,new DyedItemColor(color));return result;}
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());String path=id.getPath(),family=null;
        for(var dye:DyeColor.values())if(path.startsWith(dye.getName()+"_")){family=path.substring(dye.getName().length()+1);break;}
        if(family==null||!id.getNamespace().equals("minecraft")||!Set.of("wool","carpet","terracotta","concrete","concrete_powder","stained_glass","stained_glass_pane","banner","bed","candle","shulker_box").contains(family))return ItemStack.EMPTY;
        var item=BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(closest(color).getName()+"_"+family));if(item==Items.AIR||item==stack.getItem())return ItemStack.EMPTY;
        var result=new ItemStack(item);result.applyComponents(stack.getComponentsPatch());return result;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(!(target instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep)||!sheep.isAlive()||sheep.isSheared()||sheep.getColor()==closest(color(stack)))return InteractionResult.PASS;
        if(!Menagerie.mayChange(player.level(),player,sheep.blockPosition()))return InteractionResult.FAIL;
        if(!player.level().isClientSide()){sheep.setColor(closest(color(stack)));player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Items.GLASS_BOTTLE)));player.level().playSound(null,sheep.blockPosition(),net.minecraft.sounds.SoundEvents.DYE_USE,net.minecraft.sounds.SoundSource.PLAYERS,1,1);}return InteractionResult.SUCCESS;
    }
    public static final class Recipe extends CustomRecipe {
        private static final Recipe INSTANCE=new Recipe();
        public static final RecipeSerializer<Recipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,Recipe>unit(INSTANCE));
        private record Match(int bottle,ItemStack result){}
        private Match find(CraftingInput input){ItemStack dye=ItemStack.EMPTY,target=ItemStack.EMPTY;int slot=-1;for(int i=0;i<input.size();i++){var s=input.getItem(i);if(s.isEmpty())continue;if(s.is(OwnerAmendments.DYE_BOTTLE)){if(!dye.isEmpty())return null;dye=s;slot=i;}else{if(!target.isEmpty())return null;target=s;}}if(dye.isEmpty()||target.isEmpty())return null;var out=recolor(target,color(dye));return out.isEmpty()?null:new Match(slot,out);}
        @Override public boolean matches(CraftingInput input,Level level){return find(input)!=null;}
        @Override public ItemStack assemble(CraftingInput input){var m=find(input);return m==null?ItemStack.EMPTY:m.result.copy();}
        @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){var out=NonNullList.withSize(input.size(),ItemStack.EMPTY);var m=find(input);if(m!=null)out.set(m.bottle,new ItemStack(Items.GLASS_BOTTLE));return out;}
        @Override public RecipeSerializer<Recipe> getSerializer(){return SERIALIZER;}
    }
}
