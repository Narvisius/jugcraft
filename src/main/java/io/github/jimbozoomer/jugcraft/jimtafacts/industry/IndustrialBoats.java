package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Three original hulls on native boat movement. Electric acceleration spends the driver's equipped JE. */
public final class IndustrialBoats {
    public static final Map<String,EntityType<Hull>> TYPES=new LinkedHashMap<>();
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static Item BROKEN;
    private IndustrialBoats(){}
    public static void register(){
        for(String material:List.of("rubber","carbon","electric")){
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimta_"+material+"_boat"));
            var builder=EntityType.Builder.<Hull>of((type,level)->new Hull(type,level,material),MobCategory.MISC).sized(1.5F,.6F).clientTrackingRange(10).updateInterval(3);
            if(material.equals("electric"))builder.fireImmune();
            TYPES.put(material,Registry.register(BuiltInRegistries.ENTITY_TYPE,key,builder.build(key)));
            var ik=ResourceKey.create(Registries.ITEM,key.identifier());ITEMS.put(material,Registry.register(BuiltInRegistries.ITEM,ik,new HullItem(new Item.Properties().setId(ik).stacksTo(1),material)));
        }
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_broken_rubber_boat"));BROKEN=Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key).stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{ITEMS.values().forEach(out::accept);out.accept(BROKEN);});
    }
    static final class HullItem extends Item {
        final String material;
        HullItem(Properties p,String material){super(p);this.material=material;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player.isSpectator()||!player.isAlive())return InteractionResult.FAIL;
            var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.ANY);if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResult.PASS;
            if(!Authority.mayChange(level,player,hit.getBlockPos()))return InteractionResult.FAIL;
            var entity=TYPES.get(material).create(level,EntitySpawnReason.SPAWN_ITEM_USE);if(entity==null)return InteractionResult.FAIL;
            var at=hit.getLocation();entity.setInitialPos(at.x,at.y,at.z);entity.setYRot(player.getYRot());entity.owner=player.getUUID();
            if(!level.noCollision(entity,entity.getBoundingBox())||!level.getWorldBorder().isWithinBounds(entity.blockPosition()))return InteractionResult.FAIL;
            if(!level.isClientSide()){if(!level.addFreshEntity(entity))return InteractionResult.FAIL;player.getItemInHand(hand).consume(1,player);}
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
            out.accept(Component.literal(material.equals("electric")?"Water/lava hull. Forward thrust draws 4 JE/tick from equipped powered gear.":material.equals("rubber")?"Light rubber hull; hard crashes leave a repairable boat.":"Reinforced carbon hull; faster native rowing."));
            out.accept(Component.literal("Owner and party access. Sneak-use empty hand while empty to pick up."));
        }
    }
    public static final class Hull extends Boat {
        private static final EntityDataAccessor<Boolean> BOOST=SynchedEntityData.defineId(Hull.class,EntityDataSerializers.BOOLEAN);
        public final String material;public UUID owner;private boolean crashed;
        Hull(EntityType<? extends Hull> type,Level level,String material){super(type,level,()->ITEMS.get(material));this.material=material;}
        public boolean lavaHull(){return material.equals("electric");}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(BOOST,false);}
        public boolean permitted(Player p){return p.isAlive()&&!p.isSpectator()&&(owner==null||owner.equals(p.getUUID())||JugcraftParties.sameParty(owner,p.getUUID()));}
        @Override public InteractionResult interact(Player p,InteractionHand hand,Vec3 hit){
            if(!permitted(p))return InteractionResult.FAIL;
            if(!level().isClientSide()&&owner==null)owner=p.getUUID();
            if(p.isShiftKeyDown()&&p.getItemInHand(hand).isEmpty()&&getPassengers().isEmpty()){
                if(level() instanceof ServerLevel server&&Authority.mayChange(server,p,blockPosition())){var stack=new ItemStack(ITEMS.get(material));discard();p.getInventory().placeItemBackInInventory(stack,net.minecraft.util.Prediction.SERVER_ONLY);}
                return InteractionResult.SUCCESS;
            }
            return super.interact(p,hand,hit);
        }
        public static boolean payBoost(Player p){
            for(var slot:List.of(EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.HEAD,EquipmentSlot.FEET)){var stack=p.getItemBySlot(slot);if(stack.getItem() instanceof Chargeable&&Chargeable.drain(stack,4))return true;}return false;
        }
        @Override public void tick(){
            double speed=getDeltaMovement().horizontalDistance();
            super.tick();
            var driver=getControllingPassenger();boolean wet=isInWater()||lavaHull()&&isInLava();
            if(level() instanceof ServerLevel server){
                boolean boost=material.equals("electric")&&wet&&getPaddleState(0)&&getPaddleState(1)&&driver instanceof ServerPlayer player&&permitted(player)&&payBoost(player);
                entityData.set(BOOST,boost);
                if(driver instanceof Player player&&!permitted(player))ejectPassengers();
                if(lavaHull()){clearFire();for(var rider:getPassengers())rider.clearFire();}
                if(material.equals("rubber")&&horizontalCollision&&speed>.26&&!isRemoved()){crashed=true;destroy(server,damageSources().generic());return;}
            }
            if(wet&&driver!=null){var motion=getDeltaMovement();double factor=material.equals("carbon")?1.025:entityData.get(BOOST)?1.05:material.equals("rubber")?.99:1;
                double max=entityData.get(BOOST)?.7:material.equals("carbon")?.55:.4,scale=Math.min(factor,max/Math.max(.001,motion.horizontalDistance()));setDeltaMovement(motion.x*scale,motion.y,motion.z*scale);}
        }
        @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){if(source.getEntity() instanceof Player player&&!permitted(player))return false;return super.hurtServer(l,source,amount);}
        @Override protected void destroy(ServerLevel l,DamageSource source){if(isRemoved())return;kill(l);if(l.getGameRules().get(GameRules.ENTITY_DROPS))spawnAtLocation(l,new ItemStack(crashed?BROKEN:ITEMS.get(material)));}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(owner!=null)out.store("JimtaOwner",UUIDUtil.CODEC,owner);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);owner=in.read("JimtaOwner",UUIDUtil.CODEC).orElse(null);}
    }
}
