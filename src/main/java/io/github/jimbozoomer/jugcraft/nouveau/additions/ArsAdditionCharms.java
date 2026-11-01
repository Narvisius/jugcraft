package io.github.jimbozoomer.jugcraft.nouveau.additions;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.trinket.Wayfaring;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornTrinketItem;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/** Supplied charge counts/effects, using native equipment slots and paid shared Ley recharge. */
public final class ArsAdditionCharms {
    public enum Kind {
        FIRE_RESISTANCE(1000,5), UNDYING(1,2000), DISPEL_PROTECTION(3,1000),
        FALL_PREVENTION(20,20), WATER_BREATHING(1000,5), ENDER_MASK(100,10),
        VOID_PROTECTION(1,2000), SONIC_BOOM_PROTECTION(3,1000), WITHER_PROTECTION(10,100),
        GOLDEN(1000,5), NIGHT_VISION(20,10), POWDERED_SNOW_WALK(1000,5);
        public final int capacity, sourcePerCharge;
        Kind(int capacity,int cost){this.capacity=capacity;sourcePerCharge=cost;}
        public String id(){return name().toLowerCase(Locale.ROOT)+"_charm";}
    }
    public static final Map<Kind,Charm> ITEMS=new EnumMap<>(Kind.class);
    public static DataComponentType<Integer> CHARGES, SOURCE_CREDIT;
    private static final Map<ServerPlayer,Map<Kind,Long>> COOLDOWNS=new WeakHashMap<>();
    private ArsAdditionCharms(){}
    private static DataComponentType<Integer> component(String name,int max){return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id(name),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,max)).networkSynchronized(ByteBufCodecs.VAR_INT).build());}
    public static void register(){
        CHARGES=component("ars_charm_charges",1000);SOURCE_CREDIT=component("ars_charm_source_credit",1999);
        for(var kind:Kind.values()){
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_"+kind.id()));
            ITEMS.put(kind,Registry.register(BuiltInRegistries.ITEM,key,new Charm(new Item.Properties().setId(key).stacksTo(1).component(CHARGES,kind.capacity),kind)));
        }
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,source,amount)->{
            if(!(target instanceof ServerPlayer p)||amount<=0)return true;
            if(source.is(DamageTypeTags.IS_FIRE)&&protect(p,Kind.FIRE_RESISTANCE,Math.max(1,(int)Math.ceil(amount)),20))return false;
            if(source.is(DamageTypeTags.IS_DROWNING)&&protect(p,Kind.WATER_BREATHING,Math.max(1,(int)Math.ceil(amount)),0))return false;
            if(source.is(DamageTypes.SONIC_BOOM)&&protect(p,Kind.SONIC_BOOM_PROTECTION,1,0))return false;
            if(source.is(DamageTypes.FELL_OUT_OF_WORLD)){
                var charm=find(p,Kind.VOID_PROTECTION);var safe=charm.get(NouveauUtilities.DESTINATION);
                if(safe!=null&&NouveauUtilities.teleport(p,safe)){
                    spend(p,charm,1);p.fallDistance=0;p.setDeltaMovement(0,0,0);return false;
                }
            }return true;
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((target,source,amount)->{
            if(!(target instanceof ServerPlayer p)||!protect(p,Kind.UNDYING,1,0))return true;
            p.setHealth(1);p.removeAllEffects();p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,900,1));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,100,1));p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,800));
            p.level().broadcastEntityEvent(p,(byte)35);return false;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%10==0)for(var p:server.getPlayerList().getPlayers())pulse(p);});
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->ITEMS.values().forEach(e::accept));
    }
    /** Inventory charms match the supplied behavior; worn cosmetic copies never participate. */
    private static ItemStack find(ServerPlayer p,Kind kind){
        if(!JugNouveau.enabled()||p.isRemoved()||p.isSpectator())return ItemStack.EMPTY;
        var item=ITEMS.get(kind);if(item==null)return ItemStack.EMPTY;
        var worn=Wayfaring.worn(p,s->s.is(item)&&s.getOrDefault(CHARGES,0)>0);if(worn!=null)return worn.get();
        for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(stack.is(item)&&stack.getOrDefault(CHARGES,0)>0)return stack;}
        return ItemStack.EMPTY;
    }
    private static void spend(ServerPlayer p,ItemStack charm,int amount){if(!p.getAbilities().instabuild){charm.set(CHARGES,Math.max(0,charm.getOrDefault(CHARGES,0)-amount));p.getInventory().setChanged();}}
    public static boolean protect(LivingEntity entity,Kind kind,int cost,int interval){
        if(!(entity instanceof ServerPlayer p))return false;
        long now=p.level().getGameTime();var times=COOLDOWNS.computeIfAbsent(p,k->new EnumMap<>(Kind.class));
        // A paid window stays valid when its last charge was consumed; it cannot be extended without another charge.
        if(interval>0&&times.getOrDefault(kind,Long.MIN_VALUE)>now)return true;
        var charm=find(p,kind);if(charm.isEmpty())return false;
        spend(p,charm,cost);if(interval>0)times.put(kind,now+interval);return true;
    }
    public static boolean preventEffect(LivingEntity target,MobEffectInstance effect){return effect.is(MobEffects.WITHER)&&protect(target,Kind.WITHER_PROTECTION,1,20);}
    public static boolean snowWalk(net.minecraft.world.entity.Entity entity){
        if(entity instanceof ServerPlayer p)return protect(p,Kind.POWDERED_SNOW_WALK,1,20);
        if(!(entity instanceof Player p)||!JugNouveau.enabled())return false;
        var item=ITEMS.get(Kind.POWDERED_SNOW_WALK);if(item==null)return false;
        if(Wayfaring.worn(p,s->s.is(item)&&s.getOrDefault(CHARGES,0)>0)!=null)return true;
        for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(item)&&s.getOrDefault(CHARGES,0)>0)return true;}return false;
    }
    public static boolean preventDispel(LivingEntity target,LivingEntity caster){return target!=caster&&protect(target,Kind.DISPEL_PROTECTION,1,0);}
    private static void pulse(ServerPlayer p){
        if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator())return;
        if(p.fallDistance>3&&!p.hasEffect(MobEffects.SLOW_FALLING)&&protect(p,Kind.FALL_PREVENTION,1,0))p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,100));
        var night=p.getEffect(MobEffects.NIGHT_VISION);
        if(p.level().getMaxLocalRawBrightness(p.blockPosition())<5&&(night==null||night.getDuration()<=200)&&protect(p,Kind.NIGHT_VISION,1,0))p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,600));
        if(p.onGround()&&!p.isOnFire()&&!p.isInWater()&&!p.isInLava()&&p.level().getBlockState(p.blockPosition().below()).isSolidRender()){
            var charm=find(p,Kind.VOID_PROTECTION);if(!charm.isEmpty()){
                var safe=GlobalPos.of(p.level().dimension(),p.blockPosition());if(!safe.equals(charm.get(NouveauUtilities.DESTINATION)))charm.set(NouveauUtilities.DESTINATION,safe);
            }
        }
    }
    public static final class Charm extends WornTrinketItem {
        public final Kind kind;
        Charm(Properties properties,Kind kind){super(properties,kind.id(),Map.of());this.kind=kind;}
        @Override public InteractionResult useOn(UseOnContext context){
            if(!(context.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!JugNouveau.enabled()||p.isSpectator()||!Authority.mayChange(p.level(),p,context.getClickedPos()))return InteractionResult.FAIL;
            var stack=context.getItemInHand();int missing=kind.capacity-stack.getOrDefault(CHARGES,0);if(missing<=0)return InteractionResult.PASS;
            var be=p.level().getBlockEntity(context.getClickedPos());long available;
            if(be instanceof NouveauWorkshop jar&&jar.kind().endsWith("source_jar")&&jar.permitted(p))available=jar.ley();
            else if(be instanceof LeyPylonBlockEntity pyl&&(pyl.owner()==null||pyl.owner().equals(p.getUUID())||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(pyl.owner(),p.getUUID())))available=pyl.ley();
            else return InteractionResult.PASS;
            int credit=stack.getOrDefault(SOURCE_CREDIT,0);long ley=Math.min(available,(missing*(long)kind.sourcePerCharge-credit+99)/100);
            int charge=(int)Math.min(missing,(ley*100+credit)/kind.sourcePerCharge);if(charge==0)return InteractionResult.FAIL;
            if(be instanceof NouveauWorkshop jar)jar.draw(ley);else if(!((LeyPylonBlockEntity)be).draw(p.level(),ley))return InteractionResult.FAIL;
            stack.set(CHARGES,stack.getOrDefault(CHARGES,0)+charge);stack.set(SOURCE_CREDIT,(int)(ley*100+credit-charge*(long)kind.sourcePerCharge));
            p.sendOverlayMessage(Component.literal("Charm recharged: "+stack.get(CHARGES)+" / "+kind.capacity));return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flags){
            out.accept(Component.literal("Charges: "+stack.getOrDefault(CHARGES,0)+" / "+kind.capacity));
            out.accept(Component.literal("Carry or equip. Use on a Source Jar or Ley Pylon to recharge."));
        }
    }
}
