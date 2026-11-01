package io.github.jimbozoomer.jugcraft.menagerie;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
/** The supplied suspicious bricks hide one paid item; they are not archaeology blocks. */
public final class OwnerHiddenBricks {
    private OwnerHiddenBricks(){}
    static void register(){Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("supps_hidden_item"),Hide.SERIALIZER);}
    public static void crumble(Level l,BlockPos p,Entity entity,boolean impact){
        if(l.isClientSide()||!impact||entity.getBoundingBox().getSize()<=.5||!(entity instanceof LivingEntity||entity instanceof FallingBlockEntity))return;
        if(entity instanceof Player who){if(!Menagerie.mayChange(l,who,p))return;}else if(io.github.jimbozoomer.jugcraft.finance.LandRegistry.isClaimed(l,p)||io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(l,p))return;
        l.destroyBlock(p,false,entity);
    }
    public static VoxelShape collision(CollisionContext c){return c instanceof EntityCollisionContext ec&&ec.getEntity() instanceof Player?Block.box(0,.01,0,16,16,16):Shapes.block();}
    public static final class Crumbly extends Block {
        Crumbly(Properties p){super(p);}
        @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return collision(c);}
        @Override public void fallOn(Level l,BlockState s,BlockPos p,Entity e,double distance){super.fallOn(l,s,p,e,distance);crumble(l,p,e,!e.isSteppingCarefully()&&distance>3);}
        @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity e,InsideBlockEffectApplier effects,boolean precise){crumble(l,p,e,e.yo<e.getY());super.entityInside(s,l,p,e,effects,precise);}
    }
    public static final class Hide extends CustomRecipe {
        static final Hide INSTANCE=new Hide();static final RecipeSerializer<Hide> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,Hide>unit(INSTANCE));
        private Item resultBlock(ItemStack s){return s.is(Items.SAND)?Items.SUSPICIOUS_SAND:s.is(Items.GRAVEL)?Items.SUSPICIOUS_GRAVEL:s.is(OwnerSupplementaries.BLOCKS.get("gravel_bricks").asItem())?OwnerSupplementaries.ITEMS.get("suspicious_gravel_bricks"):null;}
        private ItemStack result(CraftingInput input){ItemStack carrier=ItemStack.EMPTY,hidden=ItemStack.EMPTY;for(int i=0;i<input.size();i++){var s=input.getItem(i);if(s.isEmpty())continue;if(carrier.isEmpty()&&resultBlock(s)!=null)carrier=s;else if(hidden.isEmpty())hidden=s;else return ItemStack.EMPTY;}
            if(carrier.isEmpty()||hidden.isEmpty()||!hidden.getItem().canFitInsideContainerItems()||hidden.has(DataComponents.CONTAINER)||hidden.has(DataComponents.BLOCK_ENTITY_DATA)||hidden.getItem() instanceof BlockItem bi&&bi.getBlock() instanceof BrushableBlock||hidden.is(OwnerSupplementaries.ITEMS.get("suspicious_gravel_bricks")))return ItemStack.EMPTY;
            var out=new ItemStack(resultBlock(carrier));out.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(java.util.List.of(hidden.copyWithCount(1))));var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("OwnerHiddenItem",true);out.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return out;
        }
        @Override public boolean matches(CraftingInput input,Level l){return !result(input).isEmpty();}
        @Override public ItemStack assemble(CraftingInput input){return result(input);}
        @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){return NonNullList.withSize(input.size(),ItemStack.EMPTY);}
        @Override public RecipeSerializer<Hide> getSerializer(){return SERIALIZER;}
    }
}
