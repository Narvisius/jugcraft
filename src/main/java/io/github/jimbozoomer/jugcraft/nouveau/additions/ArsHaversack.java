package io.github.jimbozoomer.jugcraft.nouveau.additions;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Explicit remote deposit into a loaded, presently authorized storage endpoint. Never opens loot/private slots. */
public final class ArsHaversack extends Item {
    public static ArsHaversack ITEM;
    public static net.minecraft.core.component.DataComponentType<Boolean> LOADED;
    private ArsHaversack(Properties properties){super(properties);}
    public static void register(){
        LOADED=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("ars_haversack_loaded"),net.minecraft.core.component.DataComponentType.<Boolean>builder().persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%10!=0)return;for(var p:server.getPlayerList().getPlayers())for(int i=0;i<p.getInventory().getContainerSize();i++){var bag=p.getInventory().getItem(i);if(!bag.is(ITEM))continue;if(target(p,bag)!=null)bag.set(LOADED,true);else bag.remove(LOADED);}});
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_handy_haversack"));
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,new ArsHaversack(new Properties().setId(key).stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(ITEM));
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(!c.isSecondaryUseActive())return InteractionResult.PASS;
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!JugNouveau.enabled()||NouveauStorage.accessible(p.level(),p,c.getClickedPos())==null)return InteractionResult.FAIL;
        c.getItemInHand().set(NouveauUtilities.DESTINATION,GlobalPos.of(p.level().dimension(),c.getClickedPos()));
        p.sendOverlayMessage(Component.literal("Haversack linked. Right-click with an offhand sample to set pickup filters; sneak-use toggles collection."));
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!JugNouveau.enabled()||p.isSpectator())return InteractionResult.FAIL;
        var bag=p.getItemInHand(hand);
        if(p.isShiftKeyDown()){
            boolean active=!bag.getOrDefault(NouveauVoiding.ACTIVE,false);bag.set(NouveauVoiding.ACTIVE,active);
            p.sendOverlayMessage(Component.literal("Haversack collection "+(active?"enabled":"disabled")));return InteractionResult.SUCCESS;
        }
        var sample=hand==InteractionHand.MAIN_HAND?p.getOffhandItem():p.getMainHandItem();
        if(sample.isEmpty()||sample==bag)return InteractionResult.PASS;
        NouveauVoiding.toggle(bag,sample);p.sendOverlayMessage(Component.literal("Haversack filter: "+bag.getOrDefault(NouveauUtilities.FILTER,List.of()).size()+" item types"));return InteractionResult.SUCCESS;
    }
    private static Container target(ServerPlayer p,ItemStack bag){
        var destination=bag.get(NouveauUtilities.DESTINATION);if(destination==null||!JugNouveau.enabled())return null;
        var level=p.level().getServer().getLevel(destination.dimension());if(level==null||!level.hasChunkAt(destination.pos()))return null;
        return NouveauStorage.accessibleRemote(level,p,destination.pos());
    }
    private static int deposit(ServerPlayer p,ItemStack bag,ItemStack input){
        if(input.isEmpty()||input==bag||input.is(ITEM)||p.isSpectator()||!p.isAlive())return 0;
        var storage=target(p,bag);if(storage==null)return 0;
        int original=input.getCount();
        // Server-thread transaction: each insertion is paired with shrinking the actual source stack.
        for(int i=0;i<Math.min(storage.getContainerSize(),1728)&&!input.isEmpty();i++){
            if(!storage.canPlaceItem(i,input))continue;var old=storage.getItem(i);
            int max=Math.min(storage.getMaxStackSize(),input.getMaxStackSize());
            if(old.isEmpty())storage.setItem(i,input.split(Math.min(input.getCount(),max)));
            else if(ItemStack.isSameItemSameComponents(old,input)){
                int amount=Math.min(input.getCount(),Math.max(0,max-old.getCount()));
                if(amount>0){old.grow(amount);input.shrink(amount);storage.setChanged();}
            }
        }
        return original-input.getCount();
    }
    @Override public boolean overrideStackedOnOther(ItemStack bag,Slot slot,ClickAction action,Player player){
        if(action!=ClickAction.SECONDARY||!(slot.container instanceof net.minecraft.world.entity.player.Inventory)||!slot.allowModification(player))return false;
        if(player instanceof ServerPlayer p){deposit(p,bag,slot.getItem());slot.setChanged();}return true;
    }
    @Override public boolean overrideOtherStackedOnMe(ItemStack bag,ItemStack other,Slot slot,ClickAction action,Player player,net.minecraft.world.entity.SlotAccess access){
        if(action!=ClickAction.SECONDARY||!(slot.container instanceof net.minecraft.world.entity.player.Inventory)||!slot.allowModification(player))return false;
        if(player instanceof ServerPlayer p){deposit(p,bag,other);access.set(other);}return true;
    }
    public static boolean pickup(ServerPlayer p,ItemEntity entity){
        if(ITEM==null||!JugNouveau.enabled()||entity.hasPickUpDelay()||!entity.isAlive()||p.isSpectator()||!p.isAlive()||!Authority.mayChange(p.level(),p,entity.blockPosition()))return false;
        var owner=((AlchemicalItemAccess)entity).jugcraft$pickupTarget();if(owner!=null&&!owner.equals(p.getUUID()))return false;
        var value=entity.getItem();String id=BuiltInRegistries.ITEM.getKey(value.getItem()).toString();int moved=0;
        for(int i=0;i<36&&!value.isEmpty();i++){
            var bag=p.getInventory().getItem(i);if(!bag.is(ITEM)||!bag.getOrDefault(NouveauVoiding.ACTIVE,false)||!bag.getOrDefault(NouveauUtilities.FILTER,List.of()).contains(id))continue;
            moved+=deposit(p,bag,value);
        }
        if(moved==0)return false;p.take(entity,moved);
        if(value.isEmpty()){entity.discard();return true;}entity.setItem(value);return false;
    }
}
