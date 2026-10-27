package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Source shields, two-endpoint portals and breakable restraints, without NeoForge multipart entities. */
public final class JimtaWardEntity extends Entity {
    public static final Map<String,EntityType<JimtaWardEntity>> TYPES=new LinkedHashMap<>();
    private static final EntityDataAccessor<Integer> TARGET=SynchedEntityData.defineId(JimtaWardEntity.class,EntityDataSerializers.INT);
    private static final Map<ServerPlayer,UUID> FIRST=new WeakHashMap<>();
    private static final Map<Entity,Long> TRAVEL=new WeakHashMap<>();
    private final String kind;
    private UUID owner,victim,linked;
    private GlobalPos destination;
    private int age,lifetime=400,hurtDelay;
    private float health=15;
    public JimtaWardEntity(EntityType<?> type,Level level,String kind){super(type,level);this.kind=kind;setNoGravity(true);}
    public String kind(){return kind;}
    public Entity victim(){return level().getEntity(entityData.get(TARGET));}
    static void travelCooldown(ServerPlayer p){TRAVEL.put(p,p.level().getGameTime()+40);}
    static boolean travelCooling(ServerPlayer p){return TRAVEL.getOrDefault(p,0L)>p.level().getGameTime();}
    public static void register(){
        for(String name:List.of("shield","portal","pocket_portal","arcane_shackle")){
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_"+name));
            TYPES.put(name,Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaWardEntity>of((t,l)->new JimtaWardEntity(t,l,name),MobCategory.MISC).sized(name.equals("shield")?2.5F:1F,name.equals("shield")?2.5F:2F).clientTrackingRange(16).updateInterval(1).build(key)));
        }
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{FIRST.clear();TRAVEL.clear();});
    }
    public static boolean cast(ServerPlayer p,String name){
        var level=p.level();if(!Authority.mayChange(level,p,p.blockPosition())||level.dimension().equals(JimtaPocketRooms.DIMENSION))return false;
        if(level.getEntitiesOfClass(JimtaWardEntity.class,p.getBoundingBox().inflate(96),e->p.getUUID().equals(e.owner)).size()>=8)return false;
        var hit=level.clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(name.equals("portal")?48:3)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        var at=hit.getLocation();if(name.equals("portal")){at=ground(p,at);if(at==null)return false;}
        if(!level.hasChunkAt(BlockPos.containing(at))||!Authority.mayChange(level,p,BlockPos.containing(at)))return false;
        var e=TYPES.get(name).create(level,EntitySpawnReason.MOB_SUMMONED);if(e==null)return false;
        e.owner=p.getUUID();e.snapTo(at.x,at.y,at.z,p.getYRot(),name.equals("shield")?p.getXRot():0);
        if(name.equals("arcane_shackle")){e.setPos(p.getEyePosition());e.setDeltaMovement(p.getLookAngle().scale(1.5));e.lifetime=200;}
        if(name.equals("portal")){
            if(JimtaRecasts.isPending(p,"portal")){
                var first=find(p,FIRST.get(p));if(first==null||first.destination!=null||first.distanceToSqr(e)<4&&first.level()==level)return false;
                e.lifetime=6000;first.lifetime=first.age+6000;e.linked=first.getUUID();e.destination=GlobalPos.of(first.level().dimension(),first.blockPosition());
                first.linked=e.getUUID();first.destination=GlobalPos.of(level.dimension(),e.blockPosition());
                if(!level.addFreshEntity(e)){first.linked=null;first.destination=null;return false;}
                JimtaRecasts.consume(p,"portal");FIRST.remove(p);return true;
            }
            e.lifetime=2400;if(!level.addFreshEntity(e))return false;FIRST.put(p,e.getUUID());JimtaRecasts.begin(p,"portal",2,2400);return true;
        }
        return level.addFreshEntity(e);
    }
    private static JimtaWardEntity find(ServerPlayer p,UUID id){if(id==null)return null;for(var l:p.level().getServer().getAllLevels())if(l.getEntity(id) instanceof JimtaWardEntity e)return e;return null;}
    static Vec3 ground(ServerPlayer p,Vec3 point){
        var at=BlockPos.containing(point).above();for(int i=0;i<10;i++,at=at.below()){
            if(!p.level().hasChunkAt(at))return null;if(!p.level().getBlockState(at.below()).isCollisionShapeFullBlock(p.level(),at.below()))continue;
            var v=Vec3.atBottomCenterOf(at);return JimtaWardSpells.safe(p,p.level(),v)?v:null;
        }return null;
    }
    public static void pocketPortal(ServerPlayer p,ServerLevel level,Vec3 at){
        for(var old:level.getEntitiesOfClass(JimtaWardEntity.class,new AABB(BlockPos.containing(at)).inflate(3),e->e.kind.equals("pocket_portal")&&p.getUUID().equals(e.owner)))old.discard();
        var e=TYPES.get("pocket_portal").create(level,EntitySpawnReason.MOB_SUMMONED);if(e==null)return;e.owner=p.getUUID();e.snapTo(at.x,at.y,at.z,0,0);e.lifetime=1200;level.addFreshEntity(e);
    }
    @Override public void tick(){
        super.tick();age++;if(!(level() instanceof ServerLevel level))return;
        if(hurtDelay>0)hurtDelay--;
        var p=owner==null?null:Authority.present(level,owner);
        if(age>=lifetime||!JimtaSpellbooks.enabled()||health<=0){discard();return;}
        if(kind.equals("portal")){portal(level);return;}
        if(kind.equals("pocket_portal")){for(var player:level.getEntitiesOfClass(ServerPlayer.class,getBoundingBox(),e->owner.equals(e.getUUID())))if(TRAVEL.getOrDefault(player,0L)<level.getGameTime()){TRAVEL.put(player,level.getGameTime()+40);if(level.dimension().equals(JimtaPocketRooms.DIMENSION))JimtaPocketRooms.leave(player);else JimtaPocketRooms.enter(player);}return;}
        if(p==null||p.level()!=level||!p.isAlive()||distanceToSqr(p)>128*128||!Authority.mayChange(level,p,blockPosition())){discard();return;}
        if(kind.equals("arcane_shackle")){chain(p);return;}
        for(var shot:level.getEntitiesOfClass(Projectile.class,getBoundingBox().inflate(3),e->e.isAlive()).stream().limit(64).toList()){
            if(shot.getOwner() instanceof LivingEntity actor&&!JimtaWardSpells.permitted(p,actor))continue;
            Vec3 a=shot.position().subtract(shot.getDeltaMovement()),b=shot.position().add(shot.getDeltaMovement());
            if(intersects(a,b)){damage(4);shot.discard();}
        }
    }
    private void chain(ServerPlayer p){
        var level=p.level();if(victim==null){
            Vec3 start=position(),end=start.add(getDeltaMovement());if(!level.hasChunkAt(BlockPos.containing(end))){discard();return;}
            var hit=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            var eHit=ProjectileUtil.getEntityHitResult(this,start,hit.getLocation(),getBoundingBox().expandTowards(getDeltaMovement()).inflate(.3),e->e instanceof LivingEntity v&&JimtaWardSpells.permitted(p,v),start.distanceToSqr(hit.getLocation()));
            if(eHit!=null){var v=(LivingEntity)eHit.getEntity();victim=v.getUUID();entityData.set(TARGET,v.getId());setPos(v.position());setDeltaMovement(Vec3.ZERO);lifetime=age+220;return;}
            if(hit.getType()==HitResult.Type.BLOCK||age>40){discard();return;}setPos(end);return;
        }
        if(!(level.getEntity(victim) instanceof LivingEntity v)||!JimtaWardSpells.permitted(p,v)||distanceToSqr(v)>144){discard();return;}
        entityData.set(TARGET,v.getId());Vec3 pull=position().subtract(v.position()).scale(.015),next=v.position().add(v.getDeltaMovement().add(pull));
        if(!level.hasChunkAt(BlockPos.containing(next))||!Authority.mayChange(level,p,BlockPos.containing(next))){discard();return;}
        v.setDeltaMovement(v.getDeltaMovement().add(pull));v.syncVelocity=true;if(v.getDeltaMovement().y>=0)v.resetFallDistance();
    }
    private void portal(ServerLevel level){
        if(destination==null||linked==null)return;var target=level.getServer().getLevel(destination.dimension());
        if(target==null||!target.hasChunkAt(destination.pos())||!(target.getEntity(linked) instanceof JimtaWardEntity other))return;
        var forward=Vec3.directionFromRotation(0,other.getYRot());var at=other.position().add(forward.scale(1.2));
        if(!getUUID().equals(other.linked)||!other.kind.equals("portal"))return;
        for(var entity:level.getEntities((Entity)null,getBoundingBox(),JimtaWardEntity::portalCandidate).stream().limit(16).toList()){
            var actor=transitActor(entity,level,owner);
            if(actor==null||!Authority.mayChange(level,actor,blockPosition()))continue;
            transfer(actor,entity,target,at,other.getYRot());
        }
    }
    static boolean portalCandidate(Entity entity){
        return entity.isAlive()&&!entity.isPassenger()
            &&(entity instanceof LivingEntity||entity instanceof net.minecraft.world.entity.item.ItemEntity||vehicleKind(entity));
    }
    static ServerPlayer transitActor(Entity entity,ServerLevel level,UUID owner){
        if(entity instanceof ServerPlayer player)return player;
        if(entity.getControllingPassenger() instanceof ServerPlayer driver)return driver;
        for(var passenger:entity.getPassengers())if(passenger instanceof ServerPlayer player)return player;
        return owner==null?null:Authority.present(level,owner);
    }
    private static boolean vehicleKind(Entity e){
        if(e instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat)return true;
        return e instanceof net.minecraft.world.entity.vehicle.minecart.Minecart
            ||e instanceof net.minecraft.world.entity.vehicle.minecart.MinecartChest
            ||e instanceof net.minecraft.world.entity.vehicle.minecart.MinecartHopper
            ||e instanceof net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
    }
    private static boolean authorizedLiving(ServerPlayer actor,LivingEntity living){
        if(living instanceof Player)return living==actor;
        if(living instanceof net.minecraft.world.entity.Leashable leash&&leash.isLeashed())return false;
        if(living instanceof net.minecraft.world.entity.TamableAnimal pet&&pet.isTame()&&pet.getOwner()!=actor)return false;
        if(living instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse&&horse.isTamed()&&horse.getOwner()!=actor)return false;
        // Moving one's own companion is not an attack; friendly-fire hooks must not prevent travel.
        if(living instanceof net.minecraft.world.entity.TamableAnimal pet&&pet.getOwner()==actor
            ||living instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse&&horse.isTamed()&&horse.getOwner()==actor
            ||io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons.ownedBy(living,actor.getUUID()))return true;
        return io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(actor,actor.level(),living)
            &&io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects.mayHarm(actor,living);
    }
    /** One native move, never a manually cloned stack/entity. Private rooms have a separate entry protocol. */
    public static boolean transfer(ServerPlayer actor,Entity entity,ServerLevel target,Vec3 at,float yaw){
        if(!(entity.level() instanceof ServerLevel source)||actor.level()!=source||!actor.isAlive()||actor.isSpectator()
            ||!portalCandidate(entity)||TRAVEL.getOrDefault(entity,0L)>source.getGameTime()
            ||entity.isOnPortalCooldown()||!entity.canUsePortal(false)||!entity.canTeleport(source,target)
            ||source.dimension().equals(JimtaPocketRooms.DIMENSION)||target.dimension().equals(JimtaPocketRooms.DIMENSION))return false;
        var travelers=entity.getSelfAndPassengers().limit(9).toList();if(travelers.size()>8)return false;
        if(entity.isVehicle()&&(actor.getRootVehicle()!=entity||!travelers.contains(actor)))return false;
        if(vehicleKind(entity)&&!entity.isVehicle())return false; // Never sweep unattended vehicles/cargo into a portal.
        for(var traveler:travelers){
            if(!traveler.isAlive()||traveler.isOnPortalCooldown()||TRAVEL.getOrDefault(traveler,0L)>source.getGameTime()
                ||!traveler.canUsePortal(true)||!traveler.canTeleport(source,target))return false;
            if(traveler instanceof LivingEntity living){if(!authorizedLiving(actor,living))return false;}
            else if(traveler instanceof net.minecraft.world.entity.item.ItemEntity item){if(item.getItem().isEmpty()||item.getOwner()!=actor)return false;}
            else if(traveler!=entity||!vehicleKind(traveler))return false;
            var sourceBox=traveler.getBoundingBox();
            if(sourceBox.getXsize()>8||sourceBox.getYsize()>16||sourceBox.getZsize()>8)return false;
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(sourceBox.minX,sourceBox.minY,sourceBox.minZ),BlockPos.containing(sourceBox.maxX,sourceBox.maxY,sourceBox.maxZ)))
                if(!source.hasChunkAt(pos)||!Authority.mayChange(source,actor,pos))return false;
            // Vanilla retains each rider's offset when teleporting its root vehicle.
            var arrival=at.add(traveler.position().subtract(entity.position()));
            if(!safeArrival(actor,traveler,target,arrival))return false;
        }
        var moved=entity.teleport(new net.minecraft.world.level.portal.TeleportTransition(target,at,Vec3.ZERO,yaw,entity.getXRot(),net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        if(moved==null)return false;
        for(var original:travelers)TRAVEL.put(original,source.getGameTime()+40);
        moved.getSelfAndPassengers().limit(8).forEach(traveler->{TRAVEL.put(traveler,target.getGameTime()+40);traveler.setPortalCooldown(40);traveler.resetFallDistance();if(traveler instanceof ServerPlayer player)JimtaSpellbookCurios.afterTeleport(player);});
        return true;
    }
    public static boolean mayTransport(ServerPlayer actor,LivingEntity entity){
        if(entity instanceof Player||entity.isPassenger()||entity.isVehicle()||entity instanceof net.minecraft.world.entity.Leashable leash&&leash.isLeashed())return false;
        return authorizedLiving(actor,entity);
    }
    public static boolean safeArrival(ServerPlayer actor,Entity entity,ServerLevel target,Vec3 at){
        var box=entity.getBoundingBox().move(at.subtract(entity.position()));
        // Limit exceptionally large custom entities and inspect every occupied block, not just opposite corners.
        if(box.getXsize()>8||box.getYsize()>16||box.getZsize()>8)return false;
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
            if(!target.hasChunkAt(pos)||!target.isInWorldBounds(pos)||!target.getWorldBorder().isWithinBounds(pos)||!Authority.mayChange(target,actor,pos))return false;
        }
        return target.noCollision(entity,box)&&!target.containsAnyLiquid(box);
    }

    /** Exact source 5-by-5 half-block shield plane; rotated with the caster. */
    public boolean intersects(Vec3 start,Vec3 end){
        Vec3 a=local(start),b=local(end);if(a.z*b.z>0)return false;double denominator=a.z-b.z;if(Math.abs(denominator)<1e-8)return false;double t=a.z/denominator;if(t<0||t>1)return false;Vec3 at=a.lerp(b,t);return Math.abs(at.x)<=1.25&&Math.abs(at.y)<=1.25;
    }
    private Vec3 local(Vec3 point){return point.subtract(position()).yRot((float)Math.toRadians(getYRot())).xRot((float)Math.toRadians(getXRot()));}
    static boolean blocks(LivingEntity victim,DamageSource source,float amount){
        if(!(victim.level() instanceof ServerLevel level)||source.getSourcePosition()==null)return false;Vec3 from=source.getSourcePosition(),to=victim.getBoundingBox().getCenter();
        Vec3 delta=from.subtract(to);double length=Math.min(256,delta.length());Vec3 direction=delta.normalize();
        // Follow the last 256 blocks in bounded sections instead of querying a potentially world-sized diagonal box.
        for(int section=0;section<8&&section*32<length;section++){
            Vec3 a=to.add(direction.scale(section*32)),b=to.add(direction.scale(Math.min(length,(section+1)*32)));
            for(var shield:level.getEntitiesOfClass(JimtaWardEntity.class,new AABB(a,b).inflate(2),e->e.kind.equals("shield")&&e.health>0).stream().limit(16).toList()){
                if(shield.intersects(from,to)){shield.damage(amount);return true;}
            }
        }return false;
    }
    private void damage(float amount){health-=Math.max(0,amount);if(level() instanceof ServerLevel l)l.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY(),getZ(),12,.3,.3,.3,.1);if(health<=0)discard();}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        if(!isPickable()||hurtDelay>0||!(source.getEntity() instanceof ServerPlayer attacker)||!Authority.mayChange(level,attacker,blockPosition()))return false;
        var p=owner==null?null:Authority.present(level,owner);if(p!=null&&p!=attacker&&!JimtaWardSpells.permitted(attacker,p))return false;
        damage(amount);hurtDelay=10;return true;
    }
    @Override public boolean isPickable(){return kind.equals("shield")||kind.equals("arcane_shackle");}
    @Override public boolean isPushable(){return false;}
    @Override protected AABB makeBoundingBox(Vec3 pos){return "shield".equals(kind)?new AABB(pos.x-1.3,pos.y-1.3,pos.z-1.3,pos.x+1.3,pos.y+1.3,pos.z+1.3):super.makeBoundingBox(pos);}
    @Override public InteractionResult interact(Player player,InteractionHand hand,Vec3 hit){if(player instanceof ServerPlayer p&&p.isShiftKeyDown()&&p.getUUID().equals(owner)){discard();return InteractionResult.SUCCESS;}return InteractionResult.PASS;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(TARGET,-1);}
    @Override protected void addAdditionalSaveData(ValueOutput out){if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);if(victim!=null)out.store("victim",UUIDUtil.CODEC,victim);if(linked!=null)out.store("linked",UUIDUtil.CODEC,linked);if(destination!=null)out.store("destination",GlobalPos.CODEC,destination);out.putInt("age",age);out.putInt("lifetime",lifetime);out.putFloat("health",health);}
    @Override protected void readAdditionalSaveData(ValueInput in){owner=in.read("owner",UUIDUtil.CODEC).orElse(null);victim=in.read("victim",UUIDUtil.CODEC).orElse(null);linked=in.read("linked",UUIDUtil.CODEC).orElse(null);destination=in.read("destination",GlobalPos.CODEC).orElse(null);age=in.getIntOr("age",6001);lifetime=Math.clamp(in.getIntOr("lifetime",0),0,8400);health=in.getFloatOr("health",0);}
}
