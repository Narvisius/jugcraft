package io.github.jimbozoomer.jugcraft.concordance.assay;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.*;

/** Explicit whitelists only; one active bag per second, one conversion and one repair per storage pulse. */
public final class AlchemyAutomation {
    public static DataComponentType<List<String>> FILTER;
    public static DataComponentType<Integer> BUDGET,RESERVE,STATUS;
    public static MenuType<AlchemyAutomationMenu> MENU;
    private AlchemyAutomation() {}
    public static void register(){
        FILTER=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("alchemy_filter"),DataComponentType.<List<String>>builder()
                .persistent(Codec.STRING.listOf(0,9)).networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(9))).build());
        BUDGET=integer("alchemy_budget");RESERVE=integer("alchemy_reserve");STATUS=integer("alchemy_automation_status");
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("alchemy_automation"),new MenuType<>(AlchemyAutomationMenu::new,FeatureFlags.DEFAULT_FLAGS));
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers())if(p.tickCount%20==0)pulsePlayer(p);});
    }
    private static DataComponentType<Integer> integer(String name){return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id(name),DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());}
    public static int budget(ItemStack a){return Math.clamp(a.getOrDefault(BUDGET,16),1,64);}
    public static int reserve(ItemStack a){return Math.clamp(a.getOrDefault(RESERVE,0),0,1_000_000);}
    private static boolean stopped(ItemStack a,int reason){a.set(STATUS,reason);return false;}
    public static boolean matches(ItemStack artifact,ItemStack candidate){
        return !candidate.isEmpty()&&artifact.getOrDefault(FILTER,List.of()).contains(BuiltInRegistries.ITEM.getKey(candidate.getItem()).toString())
                &&AlchemicalWorkshopBlockEntity.material(candidate)!=null;
    }
    public static void pulsePlayer(ServerPlayer p){
        if(!AlchemicalGear.ready(p))return;
        // Open menus own their bag snapshots; never run a competing writer while a container is open.
        if(p.containerMenu!=p.inventoryMenu)return;
        int start=Math.floorMod(p.tickCount/20,36);
        for(int n=0;n<36;n++){
            var bag=p.getInventory().getItem((start+n)%36);if(!AlchemyStorage.bag(bag))continue;
            var items=NonNullList.withSize(54,ItemStack.EMPTY);bag.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(items);
            var storage=new SimpleContainer(items.toArray(ItemStack[]::new));
            if(pulse(p,storage,p.position())){var saved=NonNullList.withSize(54,ItemStack.EMPTY);for(int i=0;i<54;i++)saved.set(i,storage.getItem(i));bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(saved));p.getInventory().setChanged();}
            break;
        }
        // Carried active converters use the same whitelist. Hotbar/offhand tools are never feedstock.
        for(int i=0;i<36;i++){
            var a=p.getInventory().getItem(i);
            if(converter(a)&&a.has(AlchemicalGear.ACTIVE)){convert(p,p.getInventory(),a,9,36);break;}
        }
    }
    private static boolean converter(ItemStack a){return a.is(AlchemyStorage.ITEMS.get("gem_of_eternal_density"))||a.is(AlchemyStorage.ITEMS.get("void_ring"));}
    public static boolean pulse(ServerPlayer p,Container storage,Vec3 center){
        if(!AlchemicalGear.ready(p))return false;
        boolean changed=false,converted=false,repaired=false,gathered=false;
        for(int i=0;i<storage.getContainerSize();i++){
            var a=storage.getItem(i);if(!a.has(AlchemicalGear.ACTIVE))continue;
            if(!converted&&converter(a)){changed|=convert(p,storage,a,0,storage.getContainerSize());converted=true;}
            if(!repaired&&a.is(AlchemicalGear.TALISMAN)){
                for(int j=0;j<storage.getContainerSize();j++){
                    var tool=storage.getItem(j);if(tool.isDamaged()&&tool.has(DataComponents.REPAIRABLE)&&!tool.has(DataComponents.UNBREAKABLE)){
                        int n=(int)Math.min(Math.min(4,tool.getDamageValue()),AlchemyPower.available(p));
                        if(n>0&&AlchemyPower.pay(p,n)){tool.setDamageValue(tool.getDamageValue()-n);changed=true;}break;
                    }
                }repaired=true;
            }
            if(!gathered&&a.is(AlchemicalGear.BAND)){changed|=gather(p,storage,center);gathered=true;}
        }
        if(changed)storage.setChanged();return changed;
    }
    /** Consume only the configured item identities, never patched stacks; output must fit before any conversion. */
    public static boolean convert(ServerPlayer p,Container c,ItemStack artifact,int first,int end){
        if(!AlchemicalGear.ready(p)||!artifact.has(AlchemicalGear.ACTIVE))return stopped(artifact,1);
        if(artifact.getOrDefault(FILTER,List.of()).isEmpty())return stopped(artifact,2);
        boolean voiding=artifact.is(AlchemyStorage.ITEMS.get("void_ring"));
        var key=Identifier.tryParse(artifact.getOrDefault(AlchemyPatterns.TARGET,"minecraft:diamond"));
        var target=key==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.getValue(key));
        var targetMaterial=AlchemicalWorkshopBlockEntity.material(target);
        long price=targetMaterial!=null&&targetMaterial.formable()?Assay.form(targetMaterial,1):0;
        int output=voiding?-1:room(c,target,first,end);
        if(!voiding&&(price<=0||output<0))return stopped(artifact,output<0?3:4);
        boolean changed=false;
        for(int i=first,budget=budget(artifact);i<end&&budget>0;i++){
            var feed=c.getItem(i);if(feed==artifact||!matches(artifact,feed)||!voiding&&feed.is(target.getItem()))continue;
            var material=AlchemicalWorkshopBlockEntity.material(feed);if(material==null||!material.dissolvable())continue;
            if(!voiding&&Assay.dissolve(material,1)>=price)continue;
            int count=Math.min(budget,feed.getCount());while(count>0&&Assay.dissolve(material,count)>Assay.MAX_BALANCE-Assaying.balance(p))count--;
            long gain=Assay.dissolve(material,count);if(gain<=0)continue;
            feed.shrink(count);Assaying.setBalance(p,Assaying.balance(p)+gain);budget-=count;changed=true;
        }
        if(!voiding&&Assaying.balance(p)-reserve(artifact)>=price){
            output=room(c,target,first,end);
            if(output>=0){var out=c.getItem(output);if(out.isEmpty())c.setItem(output,target);else out.grow(1);Assaying.setBalance(p,Assaying.balance(p)-price);changed=true;}
        }
        artifact.set(STATUS,changed?0:voiding?5:Assaying.balance(p)-reserve(artifact)<price?6:5);
        if(changed)c.setChanged();return changed;
    }
    public static int room(Container c,ItemStack item,int first,int end){
        if(item.isEmpty()||!AlchemyStorage.canStore(item))return -1;
        for(int i=first;i<end;i++){var s=c.getItem(i);if(!s.isEmpty()&&ItemStack.isSameItemSameComponents(s,item)&&s.getCount()<s.getMaxStackSize())return i;}
        for(int i=first;i<end;i++)if(c.getItem(i).isEmpty())return i;return -1;
    }
    public static boolean gather(ServerPlayer p,Container c,Vec3 center){
        var drops=new ArrayList<ItemEntity>();
        p.level().getEntities(EntityTypeTest.forClass(ItemEntity.class),new AABB(center,center).inflate(6),e->e.isAlive(),drops,24);
        int moved=0;
        for(var drop:drops){
            if(moved==4)break;var stack=drop.getItem();UUID target=((AlchemicalItemAccess)drop).jugcraft$pickupTarget();
            if(drop.hasPickUpDelay()||target!=null&&!target.equals(p.getUUID())||drop.getOwner()!=null&&!drop.getOwner().getUUID().equals(p.getUUID())
                    ||!Authority.mayChange(p.level(),p,drop.blockPosition())||drop.position().distanceToSqr(center)>36
                    ||p.level().clip(new ClipContext(center.add(0,.5,0),drop.position(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS)continue;
            // Pickup filter is the existing contents: seed a slot with each wanted item. Never vacuum unrelated loot.
            boolean wanted=false;for(int i=0;i<c.getContainerSize();i++)if(ItemStack.isSameItemSameComponents(c.getItem(i),stack)){wanted=true;break;}
            if(!wanted)continue;int slot=room(c,stack,0,c.getContainerSize());if(slot<0||!AlchemyPower.pay(p,1))continue;
            var out=c.getItem(slot);int amount=Math.min(stack.getCount(),out.isEmpty()?stack.getMaxStackSize():out.getMaxStackSize()-out.getCount());
            if(out.isEmpty())c.setItem(slot,stack.copyWithCount(amount));else out.grow(amount);
            stack.shrink(amount);if(stack.isEmpty())drop.discard();else drop.setItem(stack);moved++;
        }
        if(moved>0)c.setChanged();return moved>0;
    }
}
