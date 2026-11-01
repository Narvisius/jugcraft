package io.github.jimbozoomer.jugcraft.arcane;
import net.minecraft.world.level.ClipContext;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Source bow ammunition, with per-victim claim/PvP checks for both area effects. */
public final class ArcaneArrow extends AbstractArrow {
    public static final Map<String,EntityType<ArcaneArrow>> TYPES=new LinkedHashMap<>();
    private static final EntityDataAccessor<Boolean> CLOUD=SynchedEntityData.defineId(ArcaneArrow.class,EntityDataSerializers.BOOLEAN);
    private boolean spent;
    private int cloudAge;
    private final Map<UUID,Integer> affected=new HashMap<>();
    public ArcaneArrow(EntityType<? extends ArcaneArrow> type,Level l){super(type,l);}
    private ArcaneArrow(EntityType<? extends ArcaneArrow> type,Level l,LivingEntity p,ItemStack ammo,ItemStack bow){super(type,p,l,ammo,bow);}
    static void register(){for(String name:List.of("boom_arrow","draco_arcanus_arrow")){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("forbidden_"+name));TYPES.put(name,Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<ArcaneArrow>of(ArcaneArrow::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(8).updateInterval(1).build(key)));}}
    public String kind(){return getType()==TYPES.get("boom_arrow")?"boom_arrow":"draco_arcanus_arrow";}
    public boolean cloud(){return entityData.get(CLOUD);}
    public static Item item(String name,Item.Properties p){return new ArrowItem(p){@Override public AbstractArrow createArrow(Level l,ItemStack ammo,LivingEntity owner,ItemStack weapon){return new ArcaneArrow(TYPES.get(name),l,owner,ammo,weapon);}};}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CLOUD,false);}
    @Override protected ItemStack getDefaultPickupItem(){return new ItemStack(ForbiddenArcane.item(kind()));}
    @Override protected boolean canHitEntity(Entity e){return !cloud()&&(!(e instanceof LivingEntity target)||getOwner() instanceof ServerPlayer p&&TwoHanded.allowed(p,p.level(),target))&&super.canHitEntity(e);}
    private boolean allowed(ServerPlayer p,LivingEntity target){return target!=p&&TwoHanded.allowed(p,p.level(),target)&&p.level().clip(new ClipContext(position().add(0,.1,0),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;}
    @Override protected void doPostHurtEffects(LivingEntity target){
        super.doPostHurtEffects(target);if(spent||!(getOwner() instanceof ServerPlayer p)||!TwoHanded.allowed(p,p.level(),target))return;spent=true;
        var l=p.level();setPos(target.position());
        if(kind().equals("draco_arcanus_arrow")){var haze=new ArcaneArrow(TYPES.get(kind()),l);haze.setOwner(p);haze.setPos(target.position());haze.spent=true;haze.entityData.set(CLOUD,true);haze.setNoGravity(true);haze.noPhysics=true;haze.pickup=Pickup.DISALLOWED;l.addFreshEntity(haze);l.levelEvent(2006,blockPosition(),0);return;}
        // Default source blast radius 3, no terrain destruction. Never delegate protected area damage to a raw explosion.
        var victims=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(6),LivingEntity::isAlive,victims,64);
        for(var victim:victims){double distance=distanceTo(victim);if(distance>6||!allowed(p,victim))continue;double impact=1-distance/6;if(victim.hurtServer(l,l.damageSources().explosion(this,p),(float)((impact*impact+impact)*21+1))){var push=victim.position().subtract(position()).normalize().scale(impact);victim.setDeltaMovement(victim.getDeltaMovement().add(push));victim.needsSync=true;}}
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER,getX(),getY(),getZ(),1,0,0,0,0);l.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
    }
    @Override public void tick(){
        if(!cloud()){super.tick();if(level().isClientSide()&&!isInGround())level().addParticle(kind().equals("boom_arrow")?ParticleTypes.SMOKE:net.minecraft.core.particles.PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1),getX(),getY(),getZ(),0,0,0);return;}
        baseTick();if(!(level() instanceof ServerLevel l))return;if(++cloudAge>420||!(getOwner() instanceof ServerPlayer p)||!p.isAlive()||p.level()!=l){discard();return;}
        float radius=2+5*Math.clamp(cloudAge-20,0,400)/400F;l.sendParticles(net.minecraft.core.particles.PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1),getX(),getY()+.1,getZ(),8,radius*.45,.15,radius*.45,.01);
        if(cloudAge<20||cloudAge%5!=0||!Authority.mayChange(l,p,blockPosition()))return;affected.entrySet().removeIf(e->e.getValue()+20<=cloudAge);
        var victims=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(radius,.5,radius),LivingEntity::isAlive,victims,32);
        for(var victim:victims){if(affected.size()>=256||distanceToSqr(victim)>radius*radius||affected.containsKey(victim.getUUID())||!allowed(p,victim))continue;MobEffects.INSTANT_DAMAGE.value().applyInstantaneousEffect(l,this,p,victim,1,.5);affected.put(victim.getUUID(),cloudAge);}
    }
    @Override public void playerTouch(net.minecraft.world.entity.player.Player p){if(!cloud())super.playerTouch(p);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("spent",spent);out.putBoolean("cloud",cloud());out.putInt("cloud_age",cloudAge);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);spent=in.getBooleanOr("spent",false);entityData.set(CLOUD,in.getBooleanOr("cloud",false));cloudAge=Math.clamp(in.getIntOr("cloud_age",0),0,420);if(cloud()){noPhysics=true;setNoGravity(true);setDeltaMovement(Vec3.ZERO);pickup=Pickup.DISALLOWED;}}
}
