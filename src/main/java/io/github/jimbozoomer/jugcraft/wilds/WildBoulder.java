package io.github.jimbozoomer.jugcraft.wilds;
import com.geckolib.animation.object.PlayState;
import net.minecraft.world.level.ClipContext;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Physical, persistent temporary geomancy constructs. The source terrain is never copied as loot. */
public final class WildBoulder extends Projectile implements GeoEntity {
    public static EntityType<WildBoulder> TYPE;
    public static final int RESTING=0,SHOT=1,PLATFORM=2,PILLAR=3,SLING=4;
    private static final EntityDataAccessor<Integer> MODE=SynchedEntityData.defineId(WildBoulder.class,EntityDataSerializers.INT),TIER=SynchedEntityData.defineId(WildBoulder.class,EntityDataSerializers.INT),CRUMBLE=SynchedEntityData.defineId(WildBoulder.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ACTIVE=SynchedEntityData.defineId(WildBoulder.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> HEIGHT=SynchedEntityData.defineId(WildBoulder.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<BlockState> MATERIAL=SynchedEntityData.defineId(WildBoulder.class,EntityDataSerializers.BLOCK_STATE);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private UUID trial;private int life,limit=1200,delay,regrow;private boolean fragile,descending,combatPillar;private double homeY;private float damage=10;
    public WildBoulder(EntityType<? extends WildBoulder> t,Level l){super(t,l);setNoGravity(true);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_boulder"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildBoulder>of(WildBoulder::new,MobCategory.MISC).sized(2,1).clientTrackingRange(16).updateInterval(1).build(key));}
    public static WildBoulder create(ServerLevel l,LivingEntity owner,Vec3 at,int mode,int tier,int life){if(!l.hasChunkAt(BlockPos.containing(at))||!l.getWorldBorder().isWithinBounds(BlockPos.containing(at)))return null;var e=new WildBoulder(TYPE,l);e.setOwner(owner);e.setPos(at);e.entityData.set(MODE,mode);e.entityData.set(TIER,Math.clamp(tier,1,3));e.limit=Math.clamp(life,20,9000);e.homeY=at.y;e.refreshDimensions();return e;}
    public int mode(){return entityData.get(MODE);}public int tier(){return entityData.get(TIER);}public float height(){return entityData.get(HEIGHT);}public int crumble(){return entityData.get(CRUMBLE);}public BlockState material(){return entityData.get(MATERIAL);}
    public void material(BlockState state){entityData.set(MATERIAL,state);}
    public void trial(UUID id,boolean fragile){trial=id;this.fragile=fragile;}
    public boolean belongsTo(UUID id){return id.equals(trial);}
    public void awaitPillar(){entityData.set(ACTIVE,false);}
    public boolean active(){return entityData.get(ACTIVE);}
    public void combatPillar(){combatPillar=true;}
    public void descend(){descending=true;entityData.set(CRUMBLE,0);}
    public void height(float height){entityData.set(HEIGHT,Math.clamp(height,.05F,64));refreshDimensions();}
    public void shootAt(Vec3 aim,float speed,float damage,int delay){life=0;limit=200+delay;this.damage=damage;this.delay=delay;entityData.set(MODE,SHOT);setDeltaMovement(aim.normalize().scale(speed));refreshDimensions();}
    public void sling(Vec3 aim,int delay){shootAt(aim,1.6F,4,delay);entityData.set(MODE,SLING);refreshDimensions();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(ACTIVE,true);b.define(MODE,RESTING);b.define(TIER,1);b.define(HEIGHT,1F);b.define(MATERIAL,Blocks.STONE.defaultBlockState());b.define(CRUMBLE,0);}
    @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(mode()==PILLAR?2:mode()==SLING?.6F:tier()+1,mode()==PILLAR?height():mode()==SLING?.6F:1+(tier()-1)*.5F);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key==HEIGHT||key==TIER||key==MODE)refreshDimensions();}
    @Override public boolean isPickable(){return mode()==RESTING;}
    @Override public boolean canBeCollidedWith(Entity e){return active()&&!isRemoved()&&(mode()==RESTING||mode()==PLATFORM||mode()==PILLAR)&&crumble()!=-1;}
    @Override public boolean canCollideWith(Entity e){return canBeCollidedWith(e)&&!(e instanceof WildBoulder);}
    @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){if(mode()!=RESTING||!(source.getEntity() instanceof ServerPlayer p)||distanceToSqr(p)>36||getOwner()!=p)return false;shootAt(p.getLookAngle(),1.5F/tier(),10*tier(),0);playSound(OwnerWilds.sound("geomancy.boulderhit"),1,1);return true;}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l))return;if(combatPillar){if(!(getOwner() instanceof WildSculptor sculptor)||!sculptor.isAlive()){shatter(l);return;}}else if(++life>limit){shatter(l);return;}if(delay>0){delay--;return;}
        if(trial!=null){if(!(getOwner() instanceof WildSculptor sculptor)||!sculptor.trialAlive(trial)){shatter(l);return;}if(!active()){if(descending){discard();return;}if(!sculptor.platformReached(l,getY()))return;entityData.set(ACTIVE,true);playSound(OwnerWilds.sound("geomancy.bouldercharge"),.35F,1);}
            if(descending){double dy=-Math.min(.25,Math.max(0,getY()-sculptor.origin().getY()));moveRiders(l,dy);setPos(getX(),getY()+dy,getZ());if(getY()<=sculptor.origin().getY()+.05)shatter(l);return;}}
        if(mode()==PILLAR)return;
        if(mode()==PLATFORM&&fragile){int t=crumble();if(t==-1){if(--regrow<=0){entityData.set(CRUMBLE,0);playSound(OwnerWilds.sound("geomancy.bouldercharge"),.5F,1);}return;}if(t==0){var players=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),top(),e->e.onGround(),players,1);if(!players.isEmpty()){entityData.set(CRUMBLE,35);playSound(OwnerWilds.sound("sculptor.platform_crumble"),1,1);}}else {entityData.set(CRUMBLE,t==1?-1:t-1);if(t==1)regrow=40;}return;}
        if(mode()!=SHOT&&mode()!=SLING)return;
        var caster=getOwner();if(!(caster instanceof LivingEntity owner)||!owner.isAlive()||owner.level()!=l){shatter(l);return;}
        var start=position();var end=start.add(getDeltaMovement());for(int i=0;i<=4;i++)if(!l.hasChunkAt(BlockPos.containing(start.lerp(end,i/4.0)))){shatter(l);return;}
        var block=l.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));if(block.getType()==HitResult.Type.BLOCK)end=block.getLocation();
        var targets=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().expandTowards(getDeltaMovement()).inflate(.3),e->WildSolar.canHarm(owner,e),targets,32);
        LivingEntity hit=null;double distance=Double.MAX_VALUE;for(var target:targets){var contact=target.getBoundingBox().inflate(mode()==SLING?.3:.7).clip(start,end);if(contact.isPresent()&&contact.get().distanceToSqr(start)<distance){distance=contact.get().distanceToSqr(start);hit=target;}}
        if(hit!=null){if(WildSolar.canHarm(owner,hit)&&hit.hurtServer(l,damageSources().indirectMagic(this,owner),damage))hit.knockback(.5,start.x-hit.getX(),start.z-hit.getZ(),damageSources().indirectMagic(this,owner),damage);shatter(l);return;}
        setPos(end);if(block.getType()==HitResult.Type.BLOCK||life>200)shatter(l);
    }
    private AABB top(){var b=getBoundingBox();return new AABB(b.minX+.05,b.maxY-.1,b.minZ+.05,b.maxX-.05,b.maxY+.3,b.maxZ-.05);}
    void moveRiders(ServerLevel l,double dy){var riders=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),top(),e->e!=getOwner()&&e.onGround(),riders,16);for(var e:riders){e.move(MoverType.SHULKER,new Vec3(0,dy,0));e.fallDistance=0;}}
    void shatter(ServerLevel l){l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,material()),getX(),getY()+.5,getZ(),12,.6,.4,.6,.05);discard();}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<WildBoulder>("roll",0,s->mode()==SLING?s.setAndContinue(RawAnimation.begin().thenLoop("roll")):PlayState.STOP));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("active",active());out.putInt("mode",mode());out.putInt("tier",tier());out.putInt("life",life);out.putInt("limit",limit);out.putInt("delay",delay);out.putFloat("damage",damage);out.putFloat("height",height());out.putDouble("home_y",homeY);out.putBoolean("combat_pillar",combatPillar);out.putBoolean("fragile",fragile);out.putBoolean("descending",descending);out.putInt("crumble",crumble());out.putInt("regrow",regrow);if(trial!=null)out.store("trial",UUIDUtil.CODEC,trial);out.store("material",BlockState.CODEC,material());}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);entityData.set(ACTIVE,in.getBooleanOr("active",true));entityData.set(MODE,Math.clamp(in.getIntOr("mode",0),0,4));entityData.set(TIER,Math.clamp(in.getIntOr("tier",1),1,3));regrow=Math.clamp(in.getIntOr("regrow",40),0,40);life=Math.max(0,in.getIntOr("life",0));limit=Math.clamp(in.getIntOr("limit",1200),20,9000);delay=Math.clamp(in.getIntOr("delay",0),0,100);damage=Math.clamp(in.getFloatOr("damage",10),0,100);height(in.getFloatOr("height",1));homeY=in.getDoubleOr("home_y",getY());combatPillar=in.getBooleanOr("combat_pillar",false);fragile=in.getBooleanOr("fragile",false);descending=in.getBooleanOr("descending",false);trial=in.read("trial",UUIDUtil.CODEC).orElse(null);entityData.set(CRUMBLE,Math.clamp(in.getIntOr("crumble",0),-1,35));material(in.read("material",BlockState.CODEC).orElse(Blocks.STONE.defaultBlockState()));}
}
