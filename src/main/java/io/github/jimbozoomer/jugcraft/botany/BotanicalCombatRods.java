package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.level.ClipContext;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Rod combat uses server authority and physical entities; no world-fire placement. */
public final class BotanicalCombatRods {
    static final Set<String> ITEMS=Set.of("fire_rod","missile_rod","gravity_rod");
    private static final TagKey<EntityType<?>> BLACKLIST=TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jugtania_shaded_mesa_blacklist"));
    private record Grip(ItemStack rod,UUID target,double distance){}
    private static final Map<ServerPlayer,Grip> GRIPS=new WeakHashMap<>();
    static void register(){
        BotanicalRodEffect.register();ServerLifecycleEvents.SERVER_STOPPED.register(s->GRIPS.clear());
        ServerTickEvents.END_SERVER_TICK.register(server->GRIPS.keySet().removeIf(p->!p.isAlive()||p.hasDisconnected()||!p.isUsingItem()||p.getUseItem()!=GRIPS.get(p).rod));
        AttackEntityCallback.EVENT.register((p,l,h,target,hit)->p instanceof ServerPlayer sp&&throwHeld(sp)?InteractionResult.SUCCESS:InteractionResult.PASS);
    }
    static Item create(String n,Item.Properties p){return new Rod(n,p.stacksTo(1));}
    private static boolean pay(ServerPlayer p,int amount){return p.hasInfiniteMaterials()||BotanicalEquipment.pay(p,amount);}
    private static boolean affordable(ServerPlayer p,int amount){return p.hasInfiniteMaterials()||BotanicalEquipment.available(p)>=BotanicalArmor.price(p,amount);}
    static boolean enemy(ServerPlayer p,LivingEntity e){return e!=p&&e.isAlive()&&(e instanceof Enemy||e instanceof Mob m&&m.getTarget()==p)&&Authority.mayStrike(p,e)&&Authority.mayChange(p.level(),p,e.blockPosition());}
    private static boolean movable(ServerPlayer p,Entity e){return e!=p&&e.isAlive()&&!(e instanceof Player)&&!e.isPassenger()&&!e.isVehicle()&&!e.getType().builtInRegistryHolder().is(BLACKLIST)&&!(e instanceof net.minecraft.world.entity.decoration.ArmorStand)&&e.getBbWidth()<4&&e.getBbHeight()<4&&Authority.mayChange(p.level(),p,e.blockPosition())&&(!(e instanceof LivingEntity living)||Authority.mayStrike(p,living));}
    private static Entity acquire(ServerPlayer p){
        Vec3 from=p.getEyePosition(),end=from.add(p.getLookAngle().scale(24));var hit=p.level().clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));end=hit.getLocation();
        Entity best=null;double distance=Double.MAX_VALUE;for(var e:p.level().getEntities(p,new AABB(from,end).inflate(1),e->movable(p,e))){var intercept=e.getBoundingBox().inflate(.35).clip(from,end);if(intercept.isEmpty())continue;double d=from.distanceToSqr(intercept.get());if(d<distance){distance=d;best=e;}}return best;
    }
    public static boolean throwHeld(ServerPlayer p){
        var g=GRIPS.remove(p);if(g==null)return false;var e=p.level().getEntity(g.target);
        if(e==null||!movable(p,e)||p.distanceToSqr(e)>24*24)return false;
        Vec3 motion=p.getLookAngle();if(e instanceof ItemEntity item){item.setPickUpDelay(20);double speed=BotanicalArmor.fullSet(p).equals("manaweave")?2.25:1.5;motion=motion.multiply(speed,1,speed);}else motion=motion.multiply(3,1.5,3);
        e.setDeltaMovement(motion);e.syncVelocity=true;p.stopUsingItem();p.getCooldowns().addCooldown(g.rod,10);return true;
    }
    private static void grip(ServerPlayer p,ItemStack rod){
        var g=GRIPS.get(p);Entity target=g==null?acquire(p):p.level().getEntity(g.target);
        if(target==null||!movable(p,target)||p.distanceToSqr(target)>24*24){GRIPS.remove(p);p.stopUsingItem();return;}
        double distance=g==null?(target instanceof ItemEntity?2:5.5):g.distance;
        Vec3 desired=p.getEyePosition().add(p.getLookAngle().scale(distance)).add(0,-target.getBbHeight()*.5,0),motion=desired.subtract(target.position()).scale(.3333333);
        if(motion.length()>2)motion=motion.normalize().scale(2);
        for(int i=0;i<=8;i++){var at=BlockPos.containing(target.position().add(motion.scale(i/8D)));if(!p.level().hasChunkAt(at)||!Authority.mayChange(p.level(),p,at)||!ClaimTransfers.allowed(p.level(),target.blockPosition(),at)){GRIPS.remove(p);p.stopUsingItem();return;}}
        if(!pay(p,2)){GRIPS.remove(p);p.stopUsingItem();return;}target.setDeltaMovement(motion);target.syncVelocity=true;GRIPS.put(p,new Grip(rod,target.getUUID(),distance));
        p.level().sendParticles(ParticleTypes.PORTAL,target.getX(),target.getY()+target.getBbHeight()*.5,target.getZ(),2,.15,.15,.15,0);
    }
    private static final class Rod extends Item {
        final String kind;Rod(String kind,Properties p){super(p);this.kind=kind;}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!kind.equals("fire_rod"))return super.useOn(c);if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var pos=c.getClickedPos().above();if(p.getCooldowns().isOnCooldown(c.getItemInHand())||!p.level().hasChunkAt(pos)||!Authority.mayChange(p.level(),p,pos)||!affordable(p,900))return InteractionResult.FAIL;
            if(!BotanicalRodEffect.ring(p,Vec3.atBottomCenterOf(pos)))return InteractionResult.FAIL;pay(p,900);p.getCooldowns().addCooldown(c.getItemInHand(),p.hasInfiniteMaterials()?10:BotanicalArmor.fullSet(p).equals("manaweave")?600:1200);return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult use(Level level,Player user,InteractionHand hand){if(kind.equals("fire_rod"))return InteractionResult.PASS;if(user instanceof ServerPlayer p){if(p.getCooldowns().isOnCooldown(p.getItemInHand(hand)))return InteractionResult.FAIL;p.startUsingItem(hand);}return InteractionResult.CONSUME;}
        @Override public int getUseDuration(ItemStack s,LivingEntity e){return 72000;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.BOW;}
        @Override public void onUseTick(Level l,LivingEntity user,ItemStack rod,int remaining){
            if(!(user instanceof ServerPlayer p))return;if(kind.equals("gravity_rod")){grip(p,rod);return;}
            if(!kind.equals("missile_rod")||remaining%(BotanicalArmor.fullSet(p).equals("manaweave")?1:2)!=0||!affordable(p,120))return;
            var targets=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(12),e->enemy(p,e)&&p.hasLineOfSight(e));if(targets.isEmpty())return;
            if(BotanicalRodEffect.missile(p,targets.get(p.getRandom().nextInt(targets.size()))))pay(p,120);
        }
    }
}
