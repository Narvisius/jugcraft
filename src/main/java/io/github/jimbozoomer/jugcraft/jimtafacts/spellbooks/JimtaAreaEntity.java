package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauElemental;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Source fields with persistent finite lifetimes. Every force and hit is independently protected. */
public final class JimtaAreaEntity extends Entity {
    public static final Map<String,EntityType<JimtaAreaEntity>> TYPES=new LinkedHashMap<>();
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(JimtaAreaEntity.class,EntityDataSerializers.INT);
    private final String kind;private UUID owner,target;private int age;private final Map<UUID,Integer> hits=new HashMap<>();Vec3 start;
    public JimtaAreaEntity(EntityType<?> type,Level level,String kind){super(type,level);this.kind=kind;setNoGravity(!kind.equals("ice_tomb"));}
    public String kind(){return kind;}public int age(){return Math.max(age,entityData.get(AGE));}
    public static void register(){for(var kind:List.of("black_hole","gravity_fissure","firefly_swarm","fang_swirl","sunbeam","wall_of_fire","ice_tomb")){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_"+kind));var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaAreaEntity>of((t,l)->new JimtaAreaEntity(t,l,kind),MobCategory.MISC).sized(kind.equals("ice_tomb")?1:.6F,kind.equals("ice_tomb")?2.3F:1).clientTrackingRange(16).updateInterval(1).build(key));TYPES.put(kind,type);}}
    public static JimtaAreaEntity create(ServerPlayer p,String kind,Vec3 at,LivingEntity target){if(!TYPES.containsKey(kind)||!p.level().hasChunkAt(BlockPos.containing(at))||!Authority.mayChange(p.level(),p,BlockPos.containing(at))||p.level().getEntitiesOfClass(JimtaAreaEntity.class,p.getBoundingBox().inflate(128),e->p.getUUID().equals(e.owner)).size()>=72)return null;var entity=TYPES.get(kind).create(p.level(),EntitySpawnReason.TRIGGERED);if(entity==null)return null;entity.owner=p.getUUID();entity.target=target==null?null:target.getUUID();entity.snapTo(at.x,at.y,at.z,p.getYRot(),0);entity.start=at;return p.level().addFreshEntity(entity)?entity:null;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(AGE,0);}
    @Override public void tick(){super.tick();age++;if(!(level() instanceof ServerLevel level))return;var p=owner==null?null:Authority.present(level,owner);if(p==null||!p.isAlive()||!JimtaSpellbooks.enabled()||distanceToSqr(p)>160*160||age>lifetime()){ejectPassengers();discard();return;}if(age%10==0)entityData.set(AGE,age);
        switch(kind){
            case "ice_tomb"->{if(age>2&&!hasPassenger(p)){discard();return;}setDeltaMovement(getDeltaMovement().add(0,-.08,0).multiply(.95,1,.95));move(MoverType.SELF,getDeltaMovement());if(age%20==0)p.heal(1);}
            case "black_hole","gravity_fissure"->{if(kind.equals("gravity_fissure")){Vec3 next=position().add(getDeltaMovement());if(!level.hasChunkAt(BlockPos.containing(next))||!Authority.mayChange(level,p,BlockPos.containing(next))){discard();return;}setPos(next);}double radius=kind.equals("black_hole")?6:3.5;for(var victim:targets(p,radius*2)){double d=victim.position().distanceTo(position());double factor=Math.pow(1-d/(radius*2),4)*.25;victim.setDeltaMovement(victim.getDeltaMovement().add(position().subtract(victim.getBoundingBox().getCenter()).scale(Math.max(0,factor))));victim.resetFallDistance();victim.syncVelocity=true;if(victim instanceof ServerPlayer other)other.connection.send(new ClientboundSetEntityMotionPacket(other));if(kind.equals("black_hole")&&age%10==0&&d<4)JimtaShot.hit(p,kind,victim,2,"",position());}}
            case "firefly_swarm"->{var follow=target==null?null:level.getEntity(target);if(!(follow instanceof LivingEntity living)||!living.isAlive()||!permitted(p,living)){var choices=targets(p,12);follow=choices.isEmpty()?null:choices.getFirst();target=follow==null?null:follow.getUUID();}if(follow!=null){Vec3 desired=follow.getBoundingBox().getCenter().subtract(position()).normalize().scale(.18);move(MoverType.SELF,getDeltaMovement().lerp(desired,.15));setDeltaMovement(getDeltaMovement().lerp(desired,.15));}if(age%15==0)for(var victim:targets(p,2))JimtaShot.hit(p,kind,victim,2,"",position());}
            case "sunbeam"->{if(age==15)for(var victim:targets(p,2))JimtaShot.hit(p,kind,victim,12,"",position().add(0,1,0));}
            case "wall_of_fire"->{if(age%10==0)for(var victim:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(.35,2,.35),LivingEntity::isAlive).stream().limit(16).toList())JimtaShot.hit(p,kind,victim,4,"ignite",position().add(0,1,0));}
            case "fang_swirl"->{int delay=Math.clamp((int)(position().distanceTo(start)*1.5),8,40)*2/3;if(age<delay&&age%2==0)fang(p,start.lerp(position(),(double)age/delay));else if(age>=delay&&age%3==0){double angle=(age-delay)*.25;for(int i=0;i<3;i++)fang(p,position().add(Math.cos(angle+i*Math.PI*2/3)*5,0,Math.sin(angle+i*Math.PI*2/3)*5));}}
        }
    }
    private void fang(ServerPlayer p,Vec3 at){Vec3 ground=JimtaAreaSpells.ground(p,at);if(!p.level().hasChunkAt(BlockPos.containing(ground))||!Authority.mayChange(p.level(),p,BlockPos.containing(ground)))return;NouveauElemental.displayFang(p.level(),p,ground,(float)(age*.25),0);for(var victim:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(ground,ground).inflate(.7,1,.7),LivingEntity::isAlive).stream().limit(16).toList())if(age-hits.getOrDefault(victim.getUUID(),-100)>=10&&JimtaShot.hit(p,kind,victim,4,"",ground.add(0,.5,0)))hits.put(victim.getUUID(),age);}
    private List<LivingEntity> targets(ServerPlayer p,double radius){return level().getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(radius),e->e.isAlive()&&e.distanceToSqr(position())<radius*radius&&permitted(p,e)).stream().sorted(Comparator.comparingDouble(e->e.distanceToSqr(position()))).limit(16).toList();}
    private boolean permitted(ServerPlayer p,LivingEntity e){return !JimtaSpellcasting.ally(p,e)&&TwoHanded.allowed(p,p.level(),e)&&ConcordanceEffects.mayHarm(p,e)&&level().clip(new ClipContext(position(),e.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.BLOCK;}
    private int lifetime(){return switch(kind){case "black_hole"->600;case "gravity_fissure"->60;case "sunbeam"->16;case "firefly_swarm"->200;case "ice_tomb"->100;case "wall_of_fire"->240;default->190;};}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){if(!kind.equals("ice_tomb")||amount<=0)return false;var p=owner==null?null:Authority.present(level,owner);if(p!=null&&source.getEntity() instanceof ServerPlayer attacker&&(attacker==p||JimtaSpellcasting.ally(p,attacker)||!TwoHanded.allowed(attacker,level,p)||!ConcordanceEffects.mayHarm(attacker,p)))return false;ejectPassengers();discard();return true;}
    @Override public boolean isPickable(){return kind.equals("ice_tomb");}
    @Override protected boolean canAddPassenger(Entity passenger){return kind.equals("ice_tomb")&&passenger.getUUID().equals(owner)&&getPassengers().isEmpty();}
    @Override public Vec3 getPassengerRidingPosition(Entity passenger){return position();}
    @Override protected void addAdditionalSaveData(ValueOutput out){if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);if(target!=null)out.store("target",UUIDUtil.CODEC,target);out.putInt("age",age);if(start!=null)out.store("start",Vec3.CODEC,start);}
    @Override protected void readAdditionalSaveData(ValueInput in){owner=in.read("owner",UUIDUtil.CODEC).orElse(null);target=in.read("target",UUIDUtil.CODEC).orElse(null);age=in.getIntOr("age",601);start=in.read("start",Vec3.CODEC).orElse(position());entityData.set(AGE,age);}
}
