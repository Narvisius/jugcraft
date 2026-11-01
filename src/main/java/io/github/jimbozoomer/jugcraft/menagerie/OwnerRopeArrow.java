package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.inventory.ClickAction;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Paid rope ammunition using the supplied 32-rope default and original arrow artwork. */
public final class OwnerRopeArrow extends AbstractArrow {
    public static EntityType<OwnerRopeArrow> TYPE;
    public static final int CAPACITY=32;
    private BlockPos end;
    public OwnerRopeArrow(EntityType<? extends OwnerRopeArrow> type,Level level){super(type,level);}
    private OwnerRopeArrow(Level level,LivingEntity owner,ItemStack ammo,ItemStack bow){super(TYPE,owner,level,paid(ammo),bow);}
    private OwnerRopeArrow(Level level,Position pos,ItemStack ammo){super(TYPE,pos.x(),pos.y(),pos.z(),level,paid(ammo),null);}
    private static ItemStack paid(ItemStack ammo){var stack=ammo.copyWithCount(1);if(stack.has(DataComponents.INTANGIBLE_PROJECTILE))charges(stack,0);return stack;}
    public static int charges(ItemStack stack){return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("SuppsRopes",0),0,CAPACITY);}
    private static void charges(ItemStack stack,int amount){CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.putInt("SuppsRopes",Math.clamp(amount,0,CAPACITY)));}
    private static boolean rope(ItemStack stack){return stack.is(OwnerSupplementaries.BLOCKS.get("rope").asItem());}
    public static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("supps_rope_arrow"));
        TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerRopeArrow>of(OwnerRopeArrow::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(8).updateInterval(1).build(key));
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("supps_rope_arrow_charge"),ChargeRecipe.SERIALIZER);
    }
    @Override protected ItemStack getDefaultPickupItem(){return new ItemStack(OwnerSupplementaries.ITEMS.get("rope_arrow"));}
    @Override protected boolean canHitEntity(Entity entity){return getOwner() instanceof ServerPlayer player&&(!(entity instanceof LivingEntity living)||io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(player,player.level(),living))&&super.canHitEntity(entity);}
    @Override protected void onHitBlock(BlockHitResult hit){
        super.onHitBlock(hit);end=null;
        if(!(level() instanceof ServerLevel level)||!(getOwner() instanceof ServerPlayer player)||charges(getPickupItemStackOrigin())==0
                ||!Authority.mayChange(level,player,hit.getBlockPos()))return;
        var block=OwnerSupplementaries.BLOCKS.get("rope");
        BlockPos at=hit.getBlockPos();
        if(OwnerSupplementRopes.rope(level.getBlockState(at))){end=at;return;}
        var context=new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,new ItemStack(block),hit);
        at=context.getClickedPos();if(!allowed(level,player,at)||!context.canPlace())return;
        var next=block.getStateForPlacement(context);
        if(next==null||!next.canSurvive(level,at))return;
        if(level.setBlockAndUpdate(at,next)){end=at;spend(level);}
    }
    private boolean allowed(ServerLevel level,ServerPlayer player,BlockPos at){
        return level.isInWorldBounds(at)&&level.hasChunkAt(at)&&level.getWorldBorder().isWithinBounds(at)&&Authority.mayChange(level,player,at);
    }
    private void spend(ServerLevel level){charges(getPickupItemStackOrigin(),charges(getPickupItemStackOrigin())-1);level.playSound(null,end,SoundEvents.WOOD_PLACE,SoundSource.BLOCKS,.2F,1.7F);}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel level)||end==null||isRemoved())return;
        if(!(getOwner() instanceof ServerPlayer player)||player.level()!=level||!player.isAlive()||charges(getPickupItemStackOrigin())<=0||!isInGround()){end=null;return;}
        if(!allowed(level,player,end)||!OwnerSupplementRopes.rope(level.getBlockState(end))){end=null;return;}
        BlockPos next=end.below();int scanned=0;
        while(level.hasChunkAt(next)&&OwnerSupplementRopes.rope(level.getBlockState(next))&&scanned++<128)next=next.below();
        if(scanned>=128||!allowed(level,player,next)){end=null;return;}
        var old=level.getBlockState(next);if(!old.isAir()&&!old.is(Blocks.WATER)){end=null;return;}
        var state=HouseholdBlock.set(OwnerSupplementaries.BLOCKS.get("rope").defaultBlockState(),"waterlogged",Boolean.toString(old.is(Blocks.WATER)));
        if(level.setBlockAndUpdate(next,OwnerSupplementRopes.connections(state,level,next))){end=next;spend(level);}else end=null;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(end!=null)out.store("UnwindingRopeEnd",BlockPos.CODEC,end);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);end=in.read("UnwindingRopeEnd",BlockPos.CODEC).orElse(null);}
    public static final class Ammo extends ArrowItem {
        public Ammo(Properties p){super(p.stacksTo(1));}
        @Override public AbstractArrow createArrow(Level level,ItemStack ammo,LivingEntity owner,ItemStack bow){return new OwnerRopeArrow(level,owner,ammo,bow);}
        @Override public Projectile asProjectile(Level level,Position pos,ItemStack stack,Direction direction){var arrow=new OwnerRopeArrow(level,pos,stack);arrow.pickup=Pickup.ALLOWED;return arrow;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<net.minecraft.network.chat.Component> text,TooltipFlag flag){
            text.accept(net.minecraft.network.chat.Component.literal(charges(stack)+" / "+CAPACITY+" ropes"));
            text.accept(net.minecraft.network.chat.Component.literal("Add rope in crafting or right-click rope onto this arrow. Unwinds from an allowed impact."));
        }
        @Override public boolean isBarVisible(ItemStack stack){return true;}
        @Override public int getBarWidth(ItemStack stack){return Math.round(charges(stack)*13F/CAPACITY);}
        @Override public int getBarColor(ItemStack stack){return 7294006;}
        @Override public boolean overrideStackedOnOther(ItemStack arrow,Slot slot,ClickAction action,Player player){
            if(action!=ClickAction.PRIMARY||!rope(slot.getItem())||charges(arrow)==CAPACITY)return false;
            var taken=slot.safeTake(slot.getItem().getCount(),CAPACITY-charges(arrow),player);if(taken.isEmpty())return false;
            charges(arrow,charges(arrow)+taken.getCount());return true;
        }
        @Override public boolean overrideOtherStackedOnMe(ItemStack arrow,ItemStack held,Slot slot,ClickAction action,Player player,SlotAccess access){
            if(action!=ClickAction.SECONDARY||!slot.allowModification(player)||!rope(held)||charges(arrow)==CAPACITY)return false;
            int amount=Math.min(held.getCount(),CAPACITY-charges(arrow));held.shrink(amount);charges(arrow,charges(arrow)+amount);return true;
        }
    }
    public static final class ChargeRecipe extends CustomRecipe {
        private static final ChargeRecipe INSTANCE=new ChargeRecipe();
        static final RecipeSerializer<ChargeRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,ChargeRecipe>unit(INSTANCE));
        private ItemStack result(CraftingInput input){
            ItemStack arrow=ItemStack.EMPTY;int count=0;
            for(int i=0;i<input.size();i++){var stack=input.getItem(i);if(stack.isEmpty())continue;
                if((stack.is(Items.ARROW)||stack.getItem() instanceof Ammo)&&arrow.isEmpty())arrow=stack;
                else if(rope(stack))count++;else return ItemStack.EMPTY;
            }
            if(arrow.isEmpty()||count==0||charges(arrow)+count>CAPACITY)return ItemStack.EMPTY;
            var result=arrow.transmuteCopy(OwnerSupplementaries.ITEMS.get("rope_arrow"));result.setCount(1);charges(result,charges(arrow)+count);return result;
        }
        @Override public boolean matches(CraftingInput input,Level level){return !result(input).isEmpty();}
        @Override public ItemStack assemble(CraftingInput input){return result(input);}
        @Override public RecipeSerializer<ChargeRecipe> getSerializer(){return SERIALIZER;}
    }
}
