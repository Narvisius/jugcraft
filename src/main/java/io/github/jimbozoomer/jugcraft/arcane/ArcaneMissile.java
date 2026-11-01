package io.github.jimbozoomer.jugcraft.arcane;
import net.minecraft.world.level.ClipContext;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Charged owner wand/scepter projectiles, bounded lifetime and permission-checked impact. */
public final class ArcaneMissile extends Projectile {
    public static EntityType<ArcaneMissile> AUREAL,ENERGY;
    private Vec3 acceleration=Vec3.ZERO;
    public ArcaneMissile(EntityType<? extends Projectile> type,Level l){super(type,l);setNoGravity(true);}
    public boolean energy(){return getType()==ENERGY;}
    public static void register(){AUREAL=type("arcane_aureal_missile");ENERGY=type("arcane_energy_ball");}
    private static EntityType<ArcaneMissile> type(String name){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id(name));return Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<ArcaneMissile>of(ArcaneMissile::new,MobCategory.MISC).sized(.3F,.3F).clientTrackingRange(10).updateInterval(1).build(key));}
    private static boolean fire(ServerPlayer p,boolean energy){var shot=(energy?ENERGY:AUREAL).create(p.level(),EntitySpawnReason.TRIGGERED);if(shot==null)return false;shot.setOwner(p);shot.setPos(p.getEyePosition());shot.acceleration=p.getLookAngle().scale(.1);shot.setDeltaMovement(energy?Vec3.ZERO:p.getLookAngle().scale(1.1));if(!p.level().addFreshEntity(shot))return false;p.level().playSound(null,p.blockPosition(),ForbiddenArcane.sound(energy?"entity.energy_ball.launch":"item.magic_wand.cast"),SoundSource.PLAYERS,1,1);return true;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.store("acceleration",Vec3.CODEC,acceleration);o.putInt("age",tickCount);}
    @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);acceleration=i.read("acceleration",Vec3.CODEC).orElse(Vec3.ZERO);if(acceleration.lengthSqr()>.011)acceleration=acceleration.normalize().scale(.1);tickCount=Math.clamp(i.getIntOr("age",0),0,201);}
    @Override public void tick(){
        super.tick();var motion=getDeltaMovement();var start=position();var end=start.add(motion).add(0,energy()?-.01:0,0);
        if(level().isClientSide()){setPos(end);if(tickCount%4==0)level().addParticle(energy()?ParticleTypes.ELECTRIC_SPARK:ParticleTypes.ENCHANT,getX(),getY(),getZ(),0,0,0);return;}
        if(!(level() instanceof ServerLevel l)||!(getOwner() instanceof ServerPlayer p)||!p.isAlive()||p.level()!=l||tickCount>200||!l.hasChunkAt(BlockPos.containing(end))||!l.getWorldBorder().isWithinBounds(BlockPos.containing(end))){discard();return;}
        var wall=l.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));var stop=wall.getType()==HitResult.Type.BLOCK?wall.getLocation():end;
        LivingEntity nearest=null;double distance=Double.MAX_VALUE;
        for(var target:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().expandTowards(stop.subtract(start)).inflate(.3),e->e.isAlive()&&e!=p).stream().limit(32).toList()){
            var box=target.getBoundingBox().inflate(.15);var hit=box.contains(start)?Optional.of(start):box.clip(start,stop);if(hit.isPresent()&&hit.get().distanceToSqr(start)<distance){nearest=target;distance=hit.get().distanceToSqr(start);}}
        if(nearest!=null){if(io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(p,l,nearest)){nearest.hurtServer(l,l.damageSources().indirectMagic(this,p),energy()?5.5F:5);if(energy()){var bolt=net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(l,EntitySpawnReason.TRIGGERED);if(bolt!=null){bolt.setPos(nearest.position());bolt.setVisualOnly(true);l.addFreshEntity(bolt);}nearest.hurtServer(l,l.damageSources().lightningBolt(),5);}}discard();return;}
        setPos(stop);if(wall.getType()==HitResult.Type.BLOCK){l.playSound(null,blockPosition(),ForbiddenArcane.sound("entity.energy_ball.hit"),SoundSource.PLAYERS,.7F,1);discard();return;}
        setDeltaMovement(motion.add(acceleration).scale(isInWater()?.8:.95));
    }
    @Override public boolean isPickable(){return energy();}
    @Override public boolean hurtServer(ServerLevel l,DamageSource damage,float amount){if(!energy()||!(damage.getEntity() instanceof ServerPlayer p)||!Authority.mayChange(l,p,blockPosition()))return false;setOwner(p);acceleration=p.getLookAngle().scale(.1);setDeltaMovement(p.getLookAngle());needsSync=true;return true;}
    public static final class Wand extends Item {
        private final boolean energy;
        public Wand(boolean energy,Properties p){super(p.stacksTo(1));this.energy=energy;}
        @Override public int getUseDuration(ItemStack s,LivingEntity e){return energy?30:72000;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.BOW;}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){if(!energy&&player instanceof ServerPlayer p&&p.getAttachedOrCreate(ArcaneEssences.AUREAL)<5){p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Requires 5 Aureal. Drink an Aureal bottle to replenish it."));return InteractionResult.FAIL;}player.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public boolean releaseUsing(ItemStack s,Level l,LivingEntity e,int left){if(!energy&&getUseDuration(s,e)-left>=30&&e instanceof ServerPlayer p){int current=p.getAttachedOrCreate(ArcaneEssences.AUREAL);if(current>=5&&fire(p,false)){p.setAttached(ArcaneEssences.AUREAL,current-5);return true;}}return false;}
        @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity e){if(energy&&e instanceof ServerPlayer p&&fire(p,true))p.getCooldowns().addCooldown(s,10);return s;}
    }
}
