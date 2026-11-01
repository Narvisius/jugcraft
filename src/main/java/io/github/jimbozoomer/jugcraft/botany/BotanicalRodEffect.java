package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Persistent flame circles and short-lived homing mana missiles. */
public final class BotanicalRodEffect extends Projectile {
    public static EntityType<BotanicalRodEffect> TYPE;
    private static final EntityDataAccessor<Boolean> RING=SynchedEntityData.defineId(BotanicalRodEffect.class,EntityDataSerializers.BOOLEAN);
    private UUID target;
    private Vec3 lockedTarget;
    public BotanicalRodEffect(EntityType<? extends Projectile> type,Level l){super(type,l);setNoGravity(true);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_rod_effect"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalRodEffect>of(BotanicalRodEffect::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(12).updateInterval(1).build(key));}
    static boolean ring(ServerPlayer p,Vec3 at){var e=new BotanicalRodEffect(TYPE,p.level());e.setOwner(p);e.entityData.set(RING,true);e.setPos(at);return p.level().addFreshEntity(e);}
    static boolean missile(ServerPlayer p,LivingEntity target){return missile(p,target,p.position().add((p.getRandom().nextDouble()-.5)*.1,2.4,(p.getRandom().nextDouble()-.5)*.1));}
    static boolean missile(ServerPlayer p,LivingEntity target,Vec3 origin){var e=new BotanicalRodEffect(TYPE,p.level());e.setOwner(p);e.target=target.getUUID();e.setPos(origin);return p.level().hasChunkAt(e.blockPosition())&&p.level().addFreshEntity(e);}
    static boolean gaiaMissile(BotanicalGaia boss,ServerPlayer target){
        if(!(boss.level() instanceof ServerLevel level)||!boss.accepts(target))return false;
        if(level.getEntitiesOfClass(BotanicalRodEffect.class,boss.getBoundingBox().inflate(32),e->e.getOwner()==boss).size()>=32)return false;
        var e=new BotanicalRodEffect(TYPE,level);e.setOwner(boss);e.target=target.getUUID();e.lockedTarget=target.position();
        e.setPos(boss.position().add(boss.getRandom().nextDouble()-.5,2.4,boss.getRandom().nextDouble()-.5));
        return level.hasChunkAt(e.blockPosition())&&level.addFreshEntity(e);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(RING,false);}
    @Override public void tick(){
        super.tick();boolean ring=entityData.get(RING);
        if(level().isClientSide()){
            if(ring){for(int i=0;i<6;i++){double angle=random.nextDouble()*Math.PI*2,r=4+random.nextDouble();level().addParticle(ParticleTypes.FLAME,getX()+Math.cos(angle)*r,getY(),getZ()+Math.sin(angle)*r,0,.03,0);}}
            else for(int i=0;i<5;i++){var v=position().subtract(getDeltaMovement().scale(i/5D));level().addParticle(new DustParticleOptions(0xFF66FF,.8F),v.x,v.y,v.z,0,0,0);}return;
        }
        var level=(ServerLevel)level();
        if(tickCount>(ring?300:40)){discard();return;}
        var owner=getOwner();var player=owner instanceof ServerPlayer p?p:null;var boss=owner instanceof BotanicalGaia g?g:null;
        if(player==null&&boss==null||owner==null||!owner.isAlive()||owner.level()!=level||player!=null&&player.isSpectator()){discard();return;}
        if(ring){
            if(player==null){discard();return;}
            if(tickCount>45&&tickCount%5==0)for(var e:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(5),e->e!=player&&e.isAlive()))
                if(Math.hypot(e.getX()-getX(),e.getZ()-getZ())<=5&&Authority.mayStrike(player,e)&&Authority.mayChange(level,player,e.blockPosition())&&io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(player,level,e))e.igniteForSeconds(4);
            return;
        }
        var e=target==null?null:level.getEntity(target);
        if(!(e instanceof LivingEntity victim)||boss!=null&&(!(victim instanceof ServerPlayer p)||!boss.accepts(p)||lockedTarget==null)||player!=null&&!BotanicalCombatRods.enemy(player,victim)){discard();return;}
        Vec3 diff=(boss!=null?lockedTarget:victim.getBoundingBox().getCenter()).subtract(position()),motion=diff.normalize().scale(boss!=null?.5:.6);if(tickCount<10)motion=new Vec3(motion.x,Math.abs(motion.y),motion.z);
        var next=position().add(motion);if(!level.hasChunkAt(BlockPos.containing(next))){discard();return;}setDeltaMovement(motion);
        // Ignore foliage during the ray traversal itself, so a leaf in front of
        // a wall cannot hide the wall's collision from the same step.
        var hit=level.clip(new ClipContext(position(),next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this){
            @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(net.minecraft.world.level.block.state.BlockState state,BlockGetter world,BlockPos pos){
                return state.is(net.minecraft.tags.BlockTags.LEAVES)||state.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock?net.minecraft.world.phys.shapes.Shapes.empty():super.getBlockShape(state,world,pos);
            }
        });
        boolean blocked=hit.getType()==HitResult.Type.BLOCK;
        var end=blocked?hit.getLocation():next;
        if(victim.getBoundingBox().inflate(.25).clip(position(),end).isPresent()){
            if(boss!=null)victim.hurtServer(level,new DamageSource(level.damageSources().mobAttack(boss).typeHolder(),this,boss),12);
            else if(io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(player,level,victim))victim.hurtServer(level,new DamageSource(level.damageSources().playerAttack(player).typeHolder(),this,player),7);
            discard();return;
        }
        if(blocked||boss!=null&&diff.lengthSqr()<1){discard();return;}setPos(next);
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return false;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("ring",entityData.get(RING));out.putInt("age",tickCount);if(target!=null)out.putString("target",target.toString());if(lockedTarget!=null)out.store("locked_target",Vec3.CODEC,lockedTarget);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);entityData.set(RING,in.getBooleanOr("ring",false));tickCount=Math.clamp(in.getIntOr("age",301),0,301);lockedTarget=in.read("locked_target",Vec3.CODEC).orElse(null);try{target=UUID.fromString(in.getStringOr("target",""));}catch(IllegalArgumentException e){target=null;}}
}
