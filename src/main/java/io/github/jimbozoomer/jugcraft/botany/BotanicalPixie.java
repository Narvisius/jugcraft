package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Short-lived, original-model Elementium retaliation companion. */
public final class BotanicalPixie extends PathfinderMob {
    public static EntityType<BotanicalPixie> TYPE;
    private UUID summoner,targetId;private boolean friendly,gaia;
    private float damage=4;private int effect=-1;
    public BotanicalPixie(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setNoGravity(true);}
    static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_pixie"));
        TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalPixie>of(BotanicalPixie::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(8).updateInterval(1).build(key));
        FabricDefaultAttributeRegistry.register(TYPE,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,10).add(Attributes.FLYING_SPEED,.45).add(Attributes.MOVEMENT_SPEED,.2));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim,source,base,dealt,blocked)->{
            if(dealt<=0||blocked||!(victim instanceof ServerPlayer p)||p.isSpectator()||!p.isAlive()||source.getDirectEntity() instanceof BotanicalPixie||!(source.getEntity() instanceof LivingEntity attacker)||attacker==p||!Authority.mayStrike(p,attacker)||!Authority.mayChange(p.level(),p,attacker.blockPosition()))return;
            float chance=BotanicalAccessories.has(p,"pixie_ring")?.25F:0;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))if(p.getItemBySlot(slot).getItem() instanceof BotanicalArmor.Armor a&&a.family.equals("elementium"))chance+=switch(slot){case HEAD->.11F;case CHEST->.17F;case LEGS->.15F;default->.09F;};
            boolean sword=false;for(var slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND))if(p.getItemBySlot(slot).is(Jugtania.item("elementium_sword"))){chance+=.05F;sword=true;}
            if(p.getRandom().nextFloat()>=chance||p.level().getEntitiesOfClass(BotanicalPixie.class,p.getBoundingBox().inflate(48),e->p.getUUID().equals(e.summoner)).size()>=8)return;
            var pixie=new BotanicalPixie(TYPE,p.level());pixie.summoner=p.getUUID();pixie.targetId=attacker.getUUID();pixie.damage=sword?6:4;pixie.effect=BotanicalArmor.fullSet(p).equals("elementium")?p.getRandom().nextInt(4):-1;pixie.setPos(p.getX(),p.getY()+2,p.getZ());p.level().addFreshEntity(pixie);
        });
    }
    static void friend(ServerPlayer p){
        if(p.level().getEntitiesOfClass(BotanicalPixie.class,p.getBoundingBox().inflate(32),e->p.getUUID().equals(e.summoner)).size()>=8)return;
        var pixie=new BotanicalPixie(TYPE,p.level());pixie.friendly=true;pixie.summoner=p.getUUID();pixie.setPos(p.getX(),p.getEyeY()+.5,p.getZ());p.level().addFreshEntity(pixie);
    }
    static void gaia(BotanicalGaia boss,ServerPlayer target){
        if(!(boss.level() instanceof ServerLevel level)||!boss.accepts(target)||level.getEntitiesOfClass(BotanicalPixie.class,boss.getBoundingBox().inflate(32),e->boss.getUUID().equals(e.summoner)).size()>=24)return;
        var pixie=new BotanicalPixie(TYPE,level);pixie.gaia=true;pixie.summoner=boss.getUUID();pixie.targetId=target.getUUID();pixie.damage=8;pixie.setPos(boss.getX(),boss.getY()+2,boss.getZ());level.addFreshEntity(pixie);
    }
    @Override protected void registerGoals(){}
    @Override public void tick(){
        super.tick();
        if(level().isClientSide()){for(int i=0;i<2;i++)level().addParticle(new DustParticleOptions(0xFF40E6,.4F),getX()+(random.nextDouble()-.5)*.25,getY()+.25,getZ()+(random.nextDouble()-.5)*.25,0,0,0);return;}
        var level=(ServerLevel)level();var owner=summoner==null?null:level.getPlayerByUUID(summoner);var entity=targetId==null?null:level.getEntity(targetId);
        if(gaia){
            var summoning=summoner==null?null:level.getEntity(summoner);
            if(!(summoning instanceof BotanicalGaia boss)||!(entity instanceof ServerPlayer target)||!boss.accepts(target)||tickCount>200){discard();return;}
            var toward=target.getBoundingBox().getCenter().subtract(position());
            if(toward.lengthSqr()<1&&hasLineOfSight(target)){target.hurtServer(level,new DamageSource(level.damageSources().mobAttack(boss).typeHolder(),this,boss),8);discard();return;}
            var motion=toward.normalize().scale(.35);if(!level.hasChunkAt(BlockPos.containing(position().add(motion)))){discard();return;}setDeltaMovement(motion);setYRot((float)-Math.toDegrees(Math.atan2(motion.x,motion.z)));yBodyRot=getYRot();return;
        }
        if(friendly){
            if(!(owner instanceof ServerPlayer p)||!p.isAlive()||tickCount>600||distanceToSqr(p)>4096){discard();return;}
            var destination=p.position().add(Math.cos(tickCount*.06)*1.5,1.8+Math.sin(tickCount*.04)*.4,Math.sin(tickCount*.06)*1.5);var motion=destination.subtract(position()).scale(.15);
            if(!level.hasChunkAt(BlockPos.containing(position().add(motion)))||!level.noCollision(this,getBoundingBox().move(motion))){setDeltaMovement(Vec3.ZERO);return;}setDeltaMovement(motion);setYRot((float)-Math.toDegrees(Math.atan2(motion.x,motion.z)));yBodyRot=getYRot();return;
        }
        if(!(owner instanceof ServerPlayer p)||!p.isAlive()||p.isSpectator()||!(entity instanceof LivingEntity target)||!target.isAlive()||tickCount>200||distanceToSqr(target)>4096||!Authority.mayStrike(p,target)||!Authority.mayChange(level,p,target.blockPosition())){discard();return;}
        Vec3 toward=target.getBoundingBox().getCenter().subtract(position());double distance=toward.length();
        if(distance<1&&hasLineOfSight(target)){
            if(io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(p,level,target)&&target.hurtServer(level,new DamageSource(level.damageSources().playerAttack(p).typeHolder(),this,p),damage)&&effect>=0&&!(target instanceof Player)){
                var kind=switch(effect){case 0->MobEffects.BLINDNESS;case 1->MobEffects.WITHER;case 2->MobEffects.SLOWNESS;default->MobEffects.WEAKNESS;};target.addEffect(new MobEffectInstance(kind,effect==1?50:40),p);
            }discard();return;
        }
        Vec3 motion=toward.normalize().scale(Math.min(.45,distance*.15));if(!level.hasChunkAt(BlockPos.containing(position().add(motion)))){discard();return;}
        setDeltaMovement(motion);setYRot((float)-Math.toDegrees(Math.atan2(motion.x,motion.z)));yBodyRot=getYRot();
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){if(summoner!=null&&source.getEntity()!=null&&summoner.equals(source.getEntity().getUUID()))return false;return super.hurtServer(level,source,amount);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("gaia",gaia);out.putBoolean("friendly",friendly);if(summoner!=null)out.putString("summoner",summoner.toString());if(targetId!=null)out.putString("target",targetId.toString());out.putFloat("pixie_damage",damage);out.putInt("pixie_effect",effect);out.putInt("pixie_age",tickCount);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);gaia=in.getBooleanOr("gaia",false);friendly=in.getBooleanOr("friendly",false);try{summoner=UUID.fromString(in.getStringOr("summoner",""));String savedTarget=in.getStringOr("target","");targetId=savedTarget.isEmpty()?null:UUID.fromString(savedTarget);}catch(IllegalArgumentException e){summoner=null;targetId=null;}damage=Math.clamp(in.getFloatOr("pixie_damage",4),0,6);effect=Math.clamp(in.getIntOr("pixie_effect",-1),-1,3);tickCount=Math.clamp(in.getIntOr("pixie_age",friendly?601:201),0,friendly?601:201);}
}
