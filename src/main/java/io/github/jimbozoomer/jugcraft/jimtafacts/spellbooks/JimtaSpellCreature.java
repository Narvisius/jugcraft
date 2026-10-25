package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import org.jspecify.annotations.Nullable;

/** Two source creatures with their own original geometry and authored animation clips. */
public final class JimtaSpellCreature extends PathfinderMob implements GeoEntity {
    public static final Map<String,EntityType<JimtaSpellCreature>> TYPES=new LinkedHashMap<>();
    private final String kind;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID owner,victim;
    private long expires;
    private boolean releasing;
    public JimtaSpellCreature(EntityType<? extends PathfinderMob> type,Level level,String kind) {
        super(type,level);this.kind=kind;setNoGravity(true);setNoAi(true);
    }
    public String kind(){return kind;}
    public boolean restraining(){return kind.equals("root")&&!releasing&&isAlive()&&JimtaSpellbooks.enabled();}
    public static void register(){
        for(String kind:List.of("root","wisp")) {
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_"+kind));
            var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaSpellCreature>of((t,l)->new JimtaSpellCreature(t,l,kind),MobCategory.MISC)
                .sized(kind.equals("root")?1.2F:.4F,kind.equals("root")?.35F:.5F).clientTrackingRange(8).updateInterval(1).build(key));
            TYPES.put(kind,type);FabricDefaultAttributeRegistry.register(type,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,kind.equals("root")?40:1).add(Attributes.MOVEMENT_SPEED,0));
        }
    }
    public static boolean summon(ServerPlayer owner,LivingEntity target,String kind){
        var level=owner.level();
        if(kind.equals("root")&&target.getType().builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts/root_immune"))))return false;
        if(target==owner||!target.isAlive()||target.isPassenger()||JimtaSpellcasting.ally(owner,target)||!TwoHanded.allowed(owner,level,target)
                ||!Authority.mayStrike(level,owner,owner.getUUID(),target)||!Authority.mayChange(level,owner,target.blockPosition())
                ||!ConcordanceEffects.mayHarm(owner,target))return false;
        if(level.getEntitiesOfClass(JimtaSpellCreature.class,owner.getBoundingBox().inflate(64),e->owner.getUUID().equals(e.owner)).size()>=8)return false;
        var entity=TYPES.get(kind).create(level,EntitySpawnReason.MOB_SUMMONED);if(entity==null)return false;
        entity.owner=owner.getUUID();entity.victim=target.getUUID();entity.expires=level.getGameTime()+(kind.equals("root")?100:200);
        Vec3 at=kind.equals("root")?target.position():owner.getEyePosition().add(owner.getLookAngle().scale(1.25));
        if(!level.hasChunkAt(BlockPos.containing(at))||!Authority.mayChange(level,owner,BlockPos.containing(at)))return false;
        entity.snapTo(at.x,at.y,at.z,owner.getYRot(),0);
        if(kind.equals("wisp")&&!level.noCollision(entity)||!level.addFreshEntity(entity))return false;
        if(kind.equals("root")&&!target.startRiding(entity,true,false)){entity.release();return false;}
        return true;
    }
    @Override public void tick(){
        setDeltaMovement(Vec3.ZERO);
        super.tick();if(!(level() instanceof ServerLevel level))return;
        var player=owner==null?null:Authority.present(level,owner);var target=victim==null?null:level.getEntity(victim);
        if(!JimtaSpellbooks.enabled()||player==null||!player.isAlive()||level.getGameTime()>=expires||!(target instanceof LivingEntity living)||!living.isAlive()
                ||!TwoHanded.allowed(player,level,living)||!ConcordanceEffects.mayHarm(player,living)||JimtaSpellcasting.ally(player,living)){release();return;}
        if(kind.equals("root")) {
            setDeltaMovement(Vec3.ZERO);
            if(living.getVehicle()!=this){release();return;}
            if(tickCount%20==0&&living instanceof Mob mob)mob.setTarget(this);
            return;
        }
        if(distanceToSqr(living)>64*64){release();return;}
        Vec3 desired=living.getBoundingBox().getCenter().subtract(position()).normalize().scale(.32);
        var hit=level.clip(new ClipContext(position(),position().add(desired),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if(hit.getType()==HitResult.Type.BLOCK||!level.hasChunkAt(BlockPos.containing(position().add(desired)))){release();return;}
        setDeltaMovement(desired);move(MoverType.SELF,desired);
        if(getBoundingBox().inflate(.2).intersects(living.getBoundingBox())) {
            var cause=Cause.of(player.getUUID(),Cause.Origin.SPELL,"jugcraft:jimtafacts/wisp",ConcordanceEffects.nextSerial());
            ConcordanceEffects.apply(new ConcordanceEffects.Context(level,cause,player,new Ledger(new Ledger.Limits(1,4,0)),"wisp",position()),new EffectSpec(EffectKind.DAMAGE,Intent.HARMFUL,5,0,null,Stacking.STRONGEST,"spell_power:healing"),living);
            level.sendParticles(ParticleTypes.END_ROD,getX(),getY(),getZ(),12,.2,.2,.2,.05);release();
        }
    }
    public void release(){releasing=true;ejectPassengers();discard();}
    @Override public void die(DamageSource source){releasing=true;ejectPassengers();super.die(source);}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        if(source.getEntity() instanceof ServerPlayer p&&!TwoHanded.allowed(p,level,this))return false;
        return super.hurtServer(level,source,amount);
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean player){}
    @Override public Vec3 getPassengerRidingPosition(Entity passenger){return position();}
    @Override protected void registerGoals(){}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);if(victim!=null)out.store("victim",UUIDUtil.CODEC,victim);out.putLong("expires",expires);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);victim=in.read("victim",UUIDUtil.CODEC).orElse(null);expires=in.getLongOr("expires",0);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<JimtaSpellCreature>("source",0,state->state.setAndContinue(kind.equals("root")?RawAnimation.begin().thenPlay("emerge").thenLoop("root"):RawAnimation.begin().thenLoop("animation.wisp.flying"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
