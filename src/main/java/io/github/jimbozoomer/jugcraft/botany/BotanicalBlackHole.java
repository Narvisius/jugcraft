package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.inventory.ClickAction;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** One selected ordinary block, conserved as a bounded count; custom names/container data never disappear into a count. */
public final class BotanicalBlackHole extends Item {
    private static final int LIMIT=1_000_000_000;
    public static DataComponentType<ItemStack> BLOCK;
    public static DataComponentType<Integer> COUNT;
    public static DataComponentType<Boolean> ACTIVE;
    BotanicalBlackHole(Properties p){super(p.stacksTo(1));}
    static void register(){
        BLOCK=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_black_hole_block"),DataComponentType.<ItemStack>builder().persistent(ItemStack.CODEC).networkSynchronized(ItemStack.STREAM_CODEC).build());
        COUNT=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_black_hole_count"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,LIMIT)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        ACTIVE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_black_hole_active"),DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    }
    public static int count(ItemStack stack){return Math.clamp(stack.getOrDefault(COUNT,0),0,LIMIT);}
    private static ItemStack block(ItemStack stack){return stack.getOrDefault(BLOCK,ItemStack.EMPTY);}
    private static boolean plain(ItemStack stack){return stack.getItem() instanceof BlockItem&&!stack.has(DataComponents.CONTAINER)&&ItemStack.isSameItemSameComponents(stack,new ItemStack(stack.getItem()));}
    private static int accept(ItemStack talisman,ItemStack input,int maximum){
        if(!plain(input))return 0;var block=block(talisman);int count=count(talisman);
        if(!block.isEmpty()&&!ItemStack.isSameItemSameComponents(block,input)&&count>0)return 0;
        int take=Math.min(Math.min(maximum,input.getCount()),LIMIT-count);if(take<=0)return 0;
        talisman.set(BLOCK,input.copyWithCount(1));talisman.set(COUNT,count+take);input.shrink(take);return take;
    }
    static boolean supplies(ItemStack talisman,ItemStack requested){return talisman.getItem() instanceof BotanicalBlackHole&&count(talisman)>0&&ItemStack.isSameItemSameComponents(block(talisman),requested);}
    static boolean take(ItemStack talisman,ItemStack requested){if(!supplies(talisman,requested))return false;talisman.set(COUNT,count(talisman)-1);return true;}
    static void tick(ServerPlayer p,ItemStack talisman){
        if(!(talisman.getItem() instanceof BotanicalBlackHole)||!talisman.getOrDefault(ACTIVE,false)||p.tickCount%10!=0||block(talisman).isEmpty())return;
        for(int i=0;i<36&&count(talisman)<LIMIT;i++){var input=p.getInventory().getItem(i);if(ItemStack.isSameItemSameComponents(block(talisman),input)&&accept(talisman,input,input.getCount())>0)p.getInventory().setChanged();}
    }
    @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
        if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;var stack=p.getItemInHand(hand);
        if(p.isShiftKeyDown()&&!block(stack).isEmpty()){if(stack.getOrDefault(ACTIVE,false))stack.remove(ACTIVE);else stack.set(ACTIVE,true);p.sendOverlayMessage(Component.literal("Automatic collection "+(stack.has(ACTIVE)?"on":"off")));return InteractionResult.SUCCESS;}
        if(count(stack)>0){int amount=Math.min(count(stack),block(stack).getMaxStackSize());var output=block(stack).copyWithCount(amount);p.getInventory().add(output);int moved=amount-output.getCount();if(moved>0){stack.set(COUNT,count(stack)-moved);p.getInventory().setChanged();return InteractionResult.SUCCESS;}}
        p.sendOverlayMessage(Component.literal("Use on a block to select it. Sneak-use toggles collection; use in air withdraws a stack."));return InteractionResult.PASS;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var at=context.getClickedPos();if(!Authority.mayChange(p.level(),p,at)||!Authority.mayChange(p.level(),p,at.relative(context.getClickedFace())))return InteractionResult.FAIL;
        var stack=context.getItemInHand();
        if(count(stack)==0){var sample=new ItemStack(p.level().getBlockState(at).getBlock());if(plain(sample)){stack.set(BLOCK,sample);p.sendOverlayMessage(Component.literal("Selected ").append(sample.getHoverName()));return InteractionResult.SUCCESS;}}
        var material=block(stack);if(material.isEmpty()||count(stack)<=0&&!p.hasInfiniteMaterials())return InteractionResult.FAIL;
        var available=material.copyWithCount(1);var hit=new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),at,context.isInside());
        var result=available.useOn(new UseOnContext(p.level(),p,context.getHand(),available,hit));
        if(result.consumesAction()&&!p.hasInfiniteMaterials()&&available.isEmpty())stack.set(COUNT,count(stack)-1);return result;
    }
    @Override public boolean overrideOtherStackedOnMe(ItemStack talisman,ItemStack cursor,Slot slot,ClickAction action,Player player,SlotAccess cursorAccess){
        if(action!=ClickAction.SECONDARY||!slot.allowModification(player))return false;
        if(!cursor.isEmpty()){if(accept(talisman,cursor,cursor.getCount())==0)return false;cursorAccess.set(cursor);slot.setChanged();return true;}
        int amount=Math.min(count(talisman),block(talisman).getMaxStackSize());if(amount<=0)return false;
        if(!cursorAccess.set(block(talisman).copyWithCount(amount)))return false;talisman.set(COUNT,count(talisman)-amount);slot.setChanged();return true;
    }
    @Override public boolean overrideStackedOnOther(ItemStack talisman,Slot slot,ClickAction action,Player player){
        if(action!=ClickAction.SECONDARY||!slot.mayPickup(player)||!slot.allowModification(player))return false;
        if(slot.getItem().isEmpty()&&count(talisman)>0){
            var sample=block(talisman);if(sample.isEmpty()||!slot.mayPlace(sample))return false;
            int amount=Math.min(count(talisman),sample.getMaxStackSize());var rest=slot.safeInsert(sample.copyWithCount(amount));int moved=amount-rest.getCount();
            if(moved<=0)return false;talisman.set(COUNT,count(talisman)-moved);slot.setChanged();return true;
        }
        if(!plain(slot.getItem()))return false;
        var input=slot.getItem();if(!block(talisman).isEmpty()&&count(talisman)>0&&!ItemStack.isSameItemSameComponents(block(talisman),input))return false;
        int amount=Math.min(input.getCount(),LIMIT-count(talisman));if(amount<=0)return false;
        var taken=slot.safeTake(amount,amount,player);if(taken.isEmpty())return false;accept(talisman,taken,amount);slot.setChanged();return true;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> text,TooltipFlag flag){if(!block(stack).isEmpty())text.accept(Component.literal(count(stack)+" × ").append(block(stack).getHoverName()));text.accept(Component.literal("Sneak-use toggles collection. Right-click inventory blocks to store."));}
}
