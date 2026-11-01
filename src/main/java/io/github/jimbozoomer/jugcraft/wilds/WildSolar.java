package io.github.jimbozoomer.jugcraft.wilds;
import net.minecraft.world.level.ClipContext;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Server-owned solar attacks. No terrain damage, chunk loading or client-selected victims. */
public final class WildSolar extends Entity {
    public static EntityType<WildSolar> TYPE;
    public static final int STRIKE=0,BEAM=1,NOVA=2;
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(WildSolar.class,EntityDataSerializers.INT),AGE=SynchedEntityData.defineId(WildSolar.class,EntityDataSerializers.INT),CASTER=SynchedEntityData.defineId(WildSolar.class,EntityDataSerializers.INT);
    private UUID caster;
    private int life,limit=43;
    private final Map<UUID,Integer> lastHit=new HashMap<>();
    public WildSolar(EntityType<? extends WildSolar> type,Level level){super(type,level);setNoGravity(true);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_solar"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<WildSolar>of(WildSolar::new,MobCategory.MISC).sized(1,1).clientTrackingRange(16).updateInterval(1).build(key));}
    public static boolean spawn(ServerLevel level,LivingEntity owner,int kind,Vec3 at,int duration){
        if(!level.hasChunkAt(BlockPos.containing(at))||!level.getWorldBorder().isWithinBounds(BlockPos.containing(at)))return false;
        var effect=new WildSolar(TYPE,level);effect.caster=owner.getUUID();effect.entityData.set(CASTER,owner.getId());effect.entityData.set(KIND,kind);effect.limit=kind==STRIKE?1243:kind==NOVA?40:Math.clamp(duration,1,100);effect.setPos(at);effect.setYRot(owner.getYRot());effect.setXRot(owner.getXRot());return level.addFreshEntity(effect);
    }
    @Override public boolean hurtServer(ServerLevel l,net.minecraft.world.damagesource.DamageSource s,float amount){return false;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(KIND,0);b.define(AGE,0);b.define(CASTER,-1);}
    public int kind(){return entityData.get(KIND);}public int age(){return entityData.get(AGE);}
    public LivingEntity owner(){var e=level() instanceof ServerLevel l&&caster!=null?l.getEntity(caster):level().getEntity(entityData.get(CASTER));return e instanceof LivingEntity living?living:null;}
    static boolean canHarm(LivingEntity owner,LivingEntity target){
        if(target==owner||!target.isAlive()||target.isSpectator()||target.isAlliedTo(owner))return false;
        if(owner instanceof ServerPlayer p)return !(target instanceof TamableAnimal pet&&p.getUUID().equals((pet.getOwnerReference()==null?null:pet.getOwnerReference().getUUID())))&&TwoHanded.allowed(p,p.level(),target);
        if(owner instanceof WildUmvuthana follower&&follower.playerOwner()!=null){var p=follower.playerOwner();return p.level()==target.level()&&target!=p&&!(target instanceof WildUmvuthana ally&&p.getUUID().equals(ally.ownerId()))&&TwoHanded.allowed(p,p.level(),target);}
        return !(target instanceof WildUmvuthi)&&!(target instanceof WildUmvuthana)&&!(target instanceof net.minecraft.world.entity.npc.villager.Villager)&&!(target instanceof net.minecraft.world.entity.animal.golem.IronGolem)&&!(target instanceof TamableAnimal pet&&pet.isTame())&&(!(target instanceof net.minecraft.world.entity.player.Player p)||!p.isCreative());
    }
    public Vec3 beamEnd(){return beamHit().getLocation();}
    public BlockHitResult beamHit(){var owner=owner();double range=owner instanceof ServerPlayer?20:30;var end=position().add(getLookAngle().scale(range));
        // Trim the ray before an unloaded chunk; clip must never request terrain generation.
        Vec3 last=position();for(double d=0;d<=range;d+=1){var at=position().add(getLookAngle().scale(d));if(!level().hasChunkAt(BlockPos.containing(at)))break;last=at;}end=last;
        return level().clip(new ClipContext(position(),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
    }
    private void hit(ServerLevel level,LivingEntity owner,LivingEntity victim,float damage){if(!canHarm(owner,victim)||life-lastHit.getOrDefault(victim.getUUID(),-100)<10)return;
        if(victim.hurtServer(level,owner instanceof ServerPlayer p?damageSources().playerAttack(p):damageSources().mobAttack(owner),damage)){lastHit.put(victim.getUUID(),life);victim.igniteForSeconds(5);}
    }
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l))return;var owner=owner();if(owner==null||!owner.isAlive()||++life>limit){discard();return;}entityData.set(AGE,life);
        if(kind()==BEAM){setPos(owner.getEyePosition().add(owner.getLookAngle().scale(.8)));setYRot(owner.getYRot());setXRot(owner.getXRot());}
        if(kind()==STRIKE&&(!l.canSeeSky(blockPosition().above())||!l.hasChunkAt(blockPosition()))){discard();return;}
        if(life==1)playSound(OwnerWilds.sound(kind()==NOVA?"supernova.end":kind()==STRIKE?"sunstrike":"umvuthi.burst"),kind()==NOVA?3:1,1);
        if(kind()==STRIKE&&life!=35)return;
        Vec3 end=kind()==BEAM?beamEnd():position();double radius=kind()==STRIKE?3:kind()==NOVA?Math.sqrt(life/40.0)*5:.5;
        AABB box=kind()==BEAM?new AABB(position(),end).inflate(1):kind()==STRIKE?new AABB(getX()-radius,getY()-.5,getZ()-radius,getX()+radius,l.getMaxY()+1,getZ()+radius):getBoundingBox().inflate(radius);
        var targets=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),box,e->canHarm(owner,e),targets,64);
        for(var target:targets){if(kind()==BEAM){if(target.getBoundingBox().inflate(.35).clip(position(),end).isEmpty()&&!target.getBoundingBox().inflate(.35).contains(position()))continue;}
            else if(kind()==STRIKE){var offset=target.position().subtract(position());if(offset.horizontalDistanceSqr()>9||!l.canSeeSky(target.blockPosition().above()))continue;}
            else if(target.distanceToSqr(this)>radius*radius||!owner.hasLineOfSight(target))continue;
            hit(l,owner,target,kind()==STRIKE?4:kind()==BEAM?(owner instanceof ServerPlayer?1.875F:2.5F):(owner instanceof ServerPlayer?6.4F:8));
        }
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){if(caster!=null)out.store("caster",UUIDUtil.CODEC,caster);out.putInt("kind",kind());out.putInt("age",life);out.putInt("limit",limit);}
    @Override protected void readAdditionalSaveData(ValueInput in){caster=in.read("caster",UUIDUtil.CODEC).orElse(null);entityData.set(KIND,Math.clamp(in.getIntOr("kind",0),0,2));life=Math.clamp(in.getIntOr("age",0),0,1243);limit=Math.clamp(in.getIntOr("limit",43),1,1243);entityData.set(AGE,life);}
}
