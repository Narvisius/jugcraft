package io.github.jimbozoomer.jugcraft.enchanting;

import java.util.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.gamerules.GameRules;

/** Supplied Reforged effects adapted to Fabric; no research gates or client authority. */
public final class OwnerEnchantments {
    private OwnerEnchantments() {}
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final Map<LivingEntity,State> STATES=new WeakHashMap<>();
    private static final Identifier SPEED=Identifier.fromNamespaceAndPath("jugcraft","reforged/trailblazer"),
        FOOTING=Identifier.fromNamespaceAndPath("jugcraft","reforged/surefooted"), STAGGER=Identifier.fromNamespaceAndPath("jugcraft","reforged/stagger");
    private static final class State {
        UUID lastTarget; int hits, lastHit, retaliate, retaliationUntil, counter, counterUntil, vampirismUntil,
            secondWindUntil,lastStandUntil,recoveryUntil,lastFlight=-1000,staggerUntil,endurance,lastBreak;
        ItemStack lastTool=ItemStack.EMPTY;
    }
    private static State state(LivingEntity entity){return STATES.computeIfAbsent(entity,k->new State());}
    public static int level(LivingEntity entity,ItemStack stack,String name){
        if(stack.isEmpty())return 0;
        return entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .get(ResourceKey.create(Registries.ENCHANTMENT,Identifier.fromNamespaceAndPath("jugcraft","reforged/"+name)))
            .map(holder->EnchantmentHelper.getItemEnchantmentLevel(holder,stack)).orElse(0);
    }
    public static int armor(LivingEntity entity,String name){int result=0;for(var slot:ARMOR)result=Math.max(result,level(entity,entity.getItemBySlot(slot),name));return result;}
    public static void register(){
        OwnerEnchantmentMining.register();
        ServerLivingEntityEvents.AFTER_DAMAGE.register(OwnerEnchantments::afterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register((victim,source)->{
            if(source.getEntity() instanceof ServerPlayer p&&!(victim instanceof Player)){
                var tool=p.getMainHandItem();int rank=level(p,tool,"reaper");
                if(rank>0&&tool.isDamageableItem()&&p.getRandom().nextFloat()<.12*rank)tool.setDamageValue(Math.max(0,tool.getDamageValue()-rank));
            }STATES.remove(victim);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var p:server.getPlayerList().getPlayers())tick(p);
            for(var e:List.copyOf(STATES.keySet()))if(e!=null&&e.level() instanceof ServerLevel l&&l.getServer()==server){
                var s=STATES.get(e);if(s!=null&&s.staggerUntil>0&&e.tickCount>=s.staggerUntil){modifier(e,Attributes.ATTACK_SPEED,STAGGER,0);s.staggerUntil=0;}
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->STATES.keySet().removeIf(e->e.level() instanceof ServerLevel l&&l.getServer()==server));
    }
    /** Pure damage adjustment. State transitions are committed only after a successful hit. */
    public static float damage(LivingEntity target,DamageSource source,float amount){
        if(amount<=0||!(target.level() instanceof ServerLevel))return amount;
        if(target instanceof ServerPlayer p){
            if(source.is(DamageTypes.FLY_INTO_WALL))amount*=1-Math.min(.35,.12*armor(p,"cushioning"));
            if(source.is(DamageTypeTags.IS_FALL)){
                if(p.tickCount-state(p).lastFlight<60)amount*=1-Math.min(.3,.12*armor(p,"soft_landing"));
            }
        }
        if(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow)return OwnerEnchantmentProjectiles.damage(arrow,target,amount);
        if(source.getDirectEntity()!=source.getEntity()||!(source.getEntity() instanceof ServerPlayer p)||target instanceof Player||target==p)return amount;
        var tool=p.getMainHandItem();var s=state(p);float base=amount;double bonus=0;
        if(target.getHealth()<=target.getMaxHealth()*.3)bonus+=Math.min(.18,.05*level(p,tool,"executioner"));
        if(p.getAttackStrengthScale(.5f)>=.99&&target.getHealth()>=target.getMaxHealth())bonus+=Math.min(.15,.04*level(p,tool,"precision"));
        int hits=target.getUUID().equals(s.lastTarget)&&p.tickCount-s.lastHit<=50?s.hits:0;
        bonus+=Math.min(.2,.025*level(p,tool,"momentum")*hits);
        if(p.tickCount<s.retaliationUntil)bonus+=Math.min(.15,.06*s.retaliate);
        if(p.tickCount<s.counterUntil)bonus+=Math.min(.12,.025*s.counter);
        return (float)(base*(1+Math.min(.6,bonus)));
    }
    private static void afterDamage(LivingEntity victim,DamageSource source,float base,float damage,boolean blocked){
        if(!(victim.level() instanceof ServerLevel level))return;
        if(damage>0&&source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow)OwnerEnchantmentProjectiles.hit(arrow,victim);
        if(victim instanceof ServerPlayer p){
            var s=state(p);
            if(blocked){int n=level(p,p.getUseItem(),"counterguard");if(n>0){s.counter=n;s.counterUntil=p.tickCount+80+20*n;}}
            if(damage>0){
                s.retaliate=level(p,p.getMainHandItem(),"retaliation");s.retaliationUntil=p.tickCount+100+20*s.retaliate;
                int wind=armor(p,"second_wind"),stand=armor(p,"last_stand"),molten=armor(p,"molten_skin");
                if(damage>=4&&wind>0&&p.tickCount>=s.secondWindUntil){p.addEffect(new MobEffectInstance(MobEffects.SPEED,50,wind-1));s.secondWindUntil=p.tickCount+100;}
                if(p.getHealth()<=p.getMaxHealth()*.25&&stand>0&&p.tickCount>=s.lastStandUntil){p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,40+10*stand,0));s.lastStandUntil=p.tickCount+100;}
                if(molten>0&&source.getDirectEntity()==source.getEntity()&&source.getEntity() instanceof LivingEntity attacker&&!(attacker instanceof Player)&&Authority.mayStrike(level,p,p.getUUID(),attacker))attacker.igniteForSeconds(2+molten);
            }
        }
        if(damage<=0||!(source.getEntity() instanceof ServerPlayer p)||victim instanceof Player||victim==p)return;
        if(source.getDirectEntity()!=p)return;
        var s=state(p);var tool=p.getMainHandItem();
        s.hits=victim.getUUID().equals(s.lastTarget)&&p.tickCount-s.lastHit<=60?Math.min(4,s.hits+1):1;s.lastTarget=victim.getUUID();s.lastHit=p.tickCount;
        s.retaliationUntil=0;s.counterUntil=0;
        int frost=level(p,tool,"frostbite"),undertow=level(p,tool,"undertow"),stagger=level(p,tool,"stagger"),vamp=level(p,tool,"vampirism");
        if(frost>0&&s.hits>=3){victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,30+10*frost,0));victim.setTicksFrozen(Math.min(140,victim.getTicksFrozen()+10*frost));}
        if(undertow>0&&victim.isInWaterOrRain())victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,30+10*undertow,0));
        if(stagger>0&&p.getAttackStrengthScale(.5f)>=.99&&p.getRandom().nextFloat()<.08*stagger){victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,30,0));modifier(victim,Attributes.ATTACK_SPEED,STAGGER,-.2);state(victim).staggerUntil=victim.tickCount+30;}
        if(vamp>0&&p.tickCount>=s.vampirismUntil&&p.getRandom().nextFloat()<.12*vamp){p.heal(Math.min(2,damage*.1f*vamp));s.vampirismUntil=p.tickCount+100;}
    }
    private static void modifier(LivingEntity p,net.minecraft.core.Holder<Attribute> key,Identifier id,double amount){
        var attribute=p.getAttribute(key);if(attribute==null)return;
        var old=attribute.getModifier(id);
        if(amount==0){if(old!=null)attribute.removeModifier(id);}
        else if(old==null||old.amount()!=amount)attribute.addOrUpdateTransientModifier(new AttributeModifier(id,amount,id.equals(STAGGER)?AttributeModifier.Operation.ADD_MULTIPLIED_BASE:AttributeModifier.Operation.ADD_VALUE));
    }
    private static void tick(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator())return;var s=state(p);int tick=p.tickCount;
        int trail=armor(p,"trailblazer");modifier(p,Attributes.MOVEMENT_SPEED,SPEED,p.isSprinting()&&p.onGround()&&!p.isInWater()?Math.min(.06,.02*trail):0);
        var below=p.level().getBlockState(p.blockPosition().below());
        boolean sticky=below.is(net.minecraft.world.level.block.Blocks.SOUL_SAND)||below.is(net.minecraft.world.level.block.Blocks.SOUL_SOIL)||below.is(net.minecraft.world.level.block.Blocks.HONEY_BLOCK);
        modifier(p,Attributes.MOVEMENT_SPEED,FOOTING,p.onGround()&&sticky?Math.min(.05,.025*armor(p,"surefooted")):0);
        if(p.isFallFlying()){s.lastFlight=tick;int glider=armor(p,"glider");var v=p.getDeltaMovement();if(glider>0&&v.y<0)p.setDeltaMovement(v.x,Math.min(0,v.y+Math.min(.018,.006*glider)),v.z);}
        if(p.horizontalCollision&&!p.onGround()&&armor(p,"wall_grip")>0&&p.getDeltaMovement().y<-.15){var v=p.getDeltaMovement();p.setDeltaMovement(v.x,-.15,v.z);}
        int frost=armor(p,"frostguard"),lungs=armor(p,"deep_lungs"),recovery=armor(p,"recovery");
        if(frost>0&&p.getTicksFrozen()>0&&tick%Math.max(2,10/Math.min(2,frost))==0)p.setTicksFrozen(p.getTicksFrozen()-1);
        if(lungs>0&&p.isUnderWater()&&p.getAirSupply()>=0&&tick%(80/Math.min(3,lungs))==0)p.setAirSupply(Math.min(p.getMaxAirSupply(),p.getAirSupply()+1));
        if(recovery>0&&tick>=s.recoveryUntil&&p.getHealth()<p.getMaxHealth()&&p.getFoodData().getFoodLevel()>=18&&p.level().getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION)){
            p.getFoodData().addExhaustion(3);p.heal(.5f);s.recoveryUntil=tick+Math.max(80,240/recovery);
        }
        if(tick%5==0)magnet(p);
    }
    private static void magnet(ServerPlayer p){
        if(io.github.jimbozoomer.jugcraft.botany.BotanicalFields.magnetBlocked(p))return;
        int rank=Math.max(level(p,p.getMainHandItem(),"magnetism"),level(p,p.getOffhandItem(),"magnetism"));if(rank<=0)return;
        double range=2+rank;int budget=32;
        var drops=p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(range),e->!e.hasPickUpDelay()&&e.isAlive());
        drops.sort(Comparator.comparingDouble(p::distanceToSqr));
        for(var drop:drops){if(budget--==0)break;if(io.github.jimbozoomer.jugcraft.botany.BotanicalFields.magnetBlocked(drop)||!Authority.mayChange(p.level(),p,drop.blockPosition())||!p.hasLineOfSight(drop))continue;
            var owner=((io.github.jimbozoomer.jugcraft.mixin.OwnerItemTargetAccess)drop).jugcraft$pickupTarget();if(owner!=null&&!owner.equals(p.getUUID()))continue;
            var delta=p.position().add(0,.5,0).subtract(drop.position());if(delta.lengthSqr()>.04)drop.setDeltaMovement(delta.normalize().scale(.18));
        }
    }
    public static void mined(ServerPlayer p,ItemStack tool){var s=state(p);s.endurance=s.lastTool==tool&&p.tickCount-s.lastBreak<100?Math.min(12,s.endurance+1):1;s.lastBreak=p.tickCount;s.lastTool=tool;}
    public static int durability(ServerPlayer p,ItemStack stack,int amount){
        if(p==null||amount<=0)return amount;int rank=level(p,stack,"endurance");if(rank==0)return amount;var s=state(p);
        double chance=s.lastTool==stack&&p.tickCount-s.lastBreak<100?Math.min(rank*.06,s.endurance*rank*.005):0;
        for(int i=0,n=amount;i<n;i++)if(p.getRandom().nextFloat()<chance)amount--;return amount;
    }
}
