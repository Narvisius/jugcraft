package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;

/** Banner-pattern flags and raising/lowering them along actual placed stick poles. */
public final class OwnerSupplementFlags {
    private OwnerSupplementFlags(){}
    public static boolean flag(BlockState state){return state.getBlock() instanceof OwnerSupplementBlock block&&block.function.equals("flag");}
    public static DyeColor color(ItemStack stack){
        if(stack.getItem() instanceof BannerItem banner)return banner.getColor();
        if(stack.getItem() instanceof BlockItem item&&flag(item.getBlock().defaultBlockState()))return DyeColor.byName(((OwnerSupplementBlock)item.getBlock()).name.substring(5),DyeColor.WHITE);
        return null;
    }
    public static BannerPatternLayers bounded(BannerPatternLayers patterns){return new BannerPatternLayers(patterns.layers().stream().limit(16).toList());}
    private static boolean pole(BlockState state){return state.is(OwnerSupplementaries.BLOCKS.get("stick"))&&HouseholdBlock.flag(state,"axis_y")&&!HouseholdBlock.flag(state,"axis_x")&&!HouseholdBlock.flag(state,"axis_z");}
    public static InteractionResult move(Level level,BlockPos pos,Player player){
        if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
        var state=level.getBlockState(pos);if(!flag(state))return InteractionResult.PASS;
        var to=pos.relative(player.isShiftKeyDown()?Direction.DOWN:Direction.UP);
        if(!level.hasChunkAt(to)||!Menagerie.mayChange(level,player,to)||!pole(level.getBlockState(to)))return InteractionResult.PASS;
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof OwnerSupplementData data){
            ItemStack saved=data.packed();var owner=data.owner;var stick=level.getBlockState(to);
            var moved=HouseholdBlock.set(state,"waterlogged",HouseholdBlock.value(stick,"waterlogged"));
            var old=HouseholdBlock.set(stick,"waterlogged",HouseholdBlock.value(state,"waterlogged"));
            // Both positions are loaded/permitted; no items are dropped during the swap.
            level.setBlock(pos,old,2);level.setBlock(to,moved,2);
            if(level.getBlockEntity(to) instanceof OwnerSupplementData target){target.owner=owner;target.restore(saved);}
            level.updateNeighborsAt(pos,old.getBlock());level.updateNeighborsAt(to,moved.getBlock());
        }return InteractionResult.SUCCESS;
    }
    static InteractionResult fromPole(Level level,BlockPos pos,Player player){
        if(!pole(level.getBlockState(pos)))return InteractionResult.PASS;
        if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
        for(var direction:new Direction[]{Direction.UP,Direction.DOWN}){
            var cursor=pos.relative(direction);
            for(int i=0;i<64&&level.hasChunkAt(cursor);i++,cursor=cursor.relative(direction)){
                var state=level.getBlockState(cursor);if(flag(state))return move(level,cursor,player);if(!pole(state))break;
            }
        }return InteractionResult.PASS;
    }
    public static final class PatternRecipe extends CustomRecipe {
        private static final PatternRecipe INSTANCE=new PatternRecipe();
        public static final RecipeSerializer<PatternRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,PatternRecipe>unit(INSTANCE));
        private record Match(int source,ItemStack patterned,ItemStack blank){}
        private Match find(CraftingInput input){
            ItemStack patterned=ItemStack.EMPTY,blank=ItemStack.EMPTY;DyeColor color=null;int source=-1;boolean hasFlag=false;
            for(int i=0;i<input.size();i++){
                var item=input.getItem(i);if(item.isEmpty())continue;var dye=color(item);if(dye==null||color!=null&&dye!=color)return null;color=dye;
                hasFlag|=!(item.getItem() instanceof BannerItem);
                var layers=item.getOrDefault(DataComponents.BANNER_PATTERNS,BannerPatternLayers.EMPTY).layers();
                if(layers.size()>6)return null;
                if(layers.isEmpty()){if(!blank.isEmpty())return null;blank=item;}
                else{if(!patterned.isEmpty())return null;patterned=item;source=i;}
            }return !hasFlag||patterned.isEmpty()||blank.isEmpty()?null:new Match(source,patterned,blank);
        }
        @Override public boolean matches(CraftingInput input,Level level){return find(input)!=null;}
        @Override public ItemStack assemble(CraftingInput input){var m=find(input);if(m==null)return ItemStack.EMPTY;var result=m.blank.copyWithCount(1);result.set(DataComponents.BANNER_PATTERNS,m.patterned.get(DataComponents.BANNER_PATTERNS));return result;}
        @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){var result=NonNullList.withSize(input.size(),ItemStack.EMPTY);var m=find(input);if(m!=null)result.set(m.source,m.patterned.copyWithCount(1));return result;}
        @Override public RecipeSerializer<PatternRecipe> getSerializer(){return SERIALIZER;}
    }
}
