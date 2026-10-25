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
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Authored hammer, falling ice and tentacle meshes with independent bounded server timelines. */
public final class JimtaConstruct extends PathfinderMob implements GeoEntity {
    public static final Set<String> SPELLS=Set.of("spectral_hammer","ice_block","sculk_tentacles");
    public static final Map<String,EntityType<JimtaConstruct>> TYPES=new LinkedHashMap<>();
    private static final TagKey<Block> HAMMER=TagKey.create(Registries.BLOCK,Jugcraft.id("jimtafacts/spectral_hammer_mineable"));
    private final String kind;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private UUID owner,target;
    private int age,airTime=25;
    private BlockPos origin=BlockPos.ZERO;
    private Direction face=Direction.UP;
    private final Set<UUID> touched=new HashSet<>();
    public JimtaConstruct(EntityType<? extends PathfinderMob> type,Level level,String kind){super(type,level);this.kind=kind;setNoAi(true);setNoGravity(true);setPermanentlyInvulnerable(true);}
    public String kind(){return kind;}
    public static void register(){
        for(String kind:List.of("spectral_hammer","ice_block","void_tentacle")){
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_"+kind));
            var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaConstruct>of((t,l)->new JimtaConstruct(t,l,kind),MobCategory.MISC).sized(kind.equals("ice_block")?2F:.8F,kind.equals("void_tentacle")?2.5F:1F).clientTrackingRange(12).updateInterval(1).build(key));
            TYPES.put(kind,type);FabricDefaultAttributeRegistry.register(type,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.MOVEMENT_SPEED,0));
        }
    }
    private static JimtaConstruct create(ServerPlayer player,String kind,Vec3 at){
        var level=player.level();if(!level.hasChunkAt(BlockPos.containing(at))||!Authority.mayChange(level,player,BlockPos.containing(at)))return null;
        if(level.getEntitiesOfClass(JimtaConstruct.class,player.getBoundingBox().inflate(96),e->player.getUUID().equals(e.owner)).size()>=16)return null;
        var entity=TYPES.get(kind).create(level,EntitySpawnReason.MOB_SUMMONED);if(entity==null)return null;
        entity.owner=player.getUUID();entity.snapTo(at.x,at.y,at.z,player.getYRot(),0);return entity;
    }
    public static boolean cast(ServerPlayer player,String spell){
        var level=player.level();var hit=level.clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(spell.equals("spectral_hammer")?16:48)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(spell.equals("spectral_hammer")){
            if(player.gameMode.getGameModeForPlayer()==GameType.ADVENTURE||hit.getType()!=HitResult.Type.BLOCK||!level.getBlockState(hit.getBlockPos()).is(HAMMER))return false;
            var at=Vec3.atCenterOf(hit.getBlockPos());if(!hit.getDirection().getAxis().isVertical())at=at.add(0,-2,0).subtract(player.getLookAngle().multiply(1,0,1).normalize().scale(1.5));else if(hit.getDirection()==Direction.DOWN)at=at.add(0,-3,0);
            var e=create(player,spell,at);if(e==null)return false;e.origin=hit.getBlockPos();e.face=hit.getDirection();e.setXRot(e.face.getAxis().isVertical()?90:0);return level.addFreshEntity(e);
        }
        var victim=JimtaSpellcasting.target(player,spell.equals("ice_block")?32:48);
        Vec3 at=victim==null?hit.getLocation():victim.position();
        if(spell.equals("ice_block")){
            for(int i=0;i<4+(victim==null?0:(int)(victim.getBbHeight()*.5));i++){var next=at.add(0,1,0);if(!level.hasChunkAt(BlockPos.containing(next))||!level.getBlockState(BlockPos.containing(next)).isAir())break;at=next;}
            var e=create(player,spell,at);if(e==null||!level.noCollision(e))return false;e.target=victim==null?null:victim.getUUID();e.airTime=victim==null?25:35;return level.addFreshEntity(e);
        }
        boolean spawned=false;
        for(int ring=1;ring<=2;ring++)for(int i=0;i<2*ring;i++){
            double angle=i*Math.PI/ring+player.getRandom().nextFloat()*.6;var pos=BlockPos.containing(at.add(Math.cos(angle)*ring*1.3,1,Math.sin(angle)*ring*1.3));
            for(int down=0;down<8&&level.hasChunkAt(pos)&&!level.getBlockState(pos.below()).isCollisionShapeFullBlock(level,pos.below());down++)pos=pos.below();
            if(!level.hasChunkAt(pos)||!level.getBlockState(pos.below()).isCollisionShapeFullBlock(level,pos.below()))continue;
            var e=create(player,"void_tentacle",Vec3.atBottomCenterOf(pos));if(e!=null)spawned|=level.addFreshEntity(e);
        }
        return spawned;
    }
    @Override public void tick(){
        Vec3 motion=getDeltaMovement();setDeltaMovement(Vec3.ZERO);super.tick();setDeltaMovement(motion);age++;
        if(!(level() instanceof ServerLevel level))return;
        var player=owner==null?null:Authority.present(level,owner);
        if(player==null||!player.isAlive()||player.level()!=level||!JimtaSpellbooks.enabled()||distanceToSqr(player)>128*128||!Authority.mayChange(level,player,blockPosition())){discard();return;}
        switch(kind){
            case "spectral_hammer"->{if(age==13)mine(player);if(age>=30)discard();}
            case "void_tentacle"->{if(age<280&&age%20==0)for(var victim:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(.5),e->e!=this).stream().limit(16).toList())damage(player,victim,8,true);if(age>=300)discard();}
            case "ice_block"->fall(player);
        }
    }
    private ConcordanceEffects.Context context(ServerPlayer p,String operation,int work){return new ConcordanceEffects.Context(p.level(),Cause.of(p.getUUID(),Cause.Origin.SPELL,"jugcraft:jimtafacts/"+kind,ConcordanceEffects.nextSerial()),p,new Ledger(new Ledger.Limits(work,work*4,work)),operation,position());}
    private void mine(ServerPlayer player){
        var level=player.level();var ctx=context(player,"hammer",18);Set<BlockPos> visited=new HashSet<>();var queue=new ArrayDeque<BlockPos>();queue.add(origin);
        while(!queue.isEmpty()&&visited.size()<64){var pos=queue.remove();if(!visited.add(pos)||!level.hasChunkAt(pos))continue;var d=pos.subtract(origin);int axial=face.getAxis()==Direction.Axis.X?d.getX():face.getAxis()==Direction.Axis.Y?d.getY():d.getZ();int direction=face.getAxisDirection().getStep();
            if(axial*direction>0||axial*direction< -1||Math.abs(d.getX())>1||Math.abs(d.getY())>1||Math.abs(d.getZ())>1||!level.getBlockState(pos).is(HAMMER))continue;
            if(ConcordanceEffects.apply(ctx,new EffectSpec(EffectKind.NOUVEAU_BLOCK,Intent.HARMFUL,3,0,"jugcraft:nouveau/break",Stacking.STRONGEST,null),pos).applied())for(var adjacent:Direction.values())queue.add(pos.relative(adjacent));
        }
    }
    private void fall(ServerPlayer player){
        var level=player.level();if(age>160){discard();return;}Vec3 movement=getDeltaMovement();var victim=target==null?null:level.getEntity(target);
        if(victim!=null&&victim.isAlive()){var diff=victim.position().subtract(position()).multiply(1,0,1);if(diff.lengthSqr()>.1)movement=movement.add(diff.normalize().scale(.025*(airTime<=0?2:1)));}
        if(airTime-->0)movement=movement.multiply(.95,.75,.95).add(0,victim!=null&&getY()-victim.getY()>3.5+victim.getBbHeight()*.5?-.005:.01,0);else movement=movement.add(0,-.15,0);
        if(airTime==0)movement=new Vec3(0,.5,0);
        if(!level.hasChunkAt(BlockPos.containing(position().add(movement)))){discard();return;}setDeltaMovement(movement);move(MoverType.SELF,movement);
        if(airTime<0){for(var living:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(.35),e->e!=this).stream().limit(16).toList())if(touched.size()<16&&touched.add(living.getUUID()))damage(player,living,7,false);
            if(onGround()){for(var living:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(3.5),e->e!=this).stream().limit(16).toList()){double dist=distanceTo(living);if(dist<3.5)damage(player,living,Math.max(1,(int)(14*(1-Math.pow(dist/3.5,3)))),false);}level.sendParticles(ParticleTypes.SNOWFLAKE,getX(),getY(),getZ(),45,1,.4,1,.1);discard();}}
    }
    private void damage(ServerPlayer player,LivingEntity victim,int amount,boolean blind){
        if(!victim.isAlive()||JimtaSpellcasting.ally(player,victim)||!TwoHanded.allowed(player,player.level(),victim)||!ConcordanceEffects.mayHarm(player,victim)||!hasLineOfSight(victim))return;
        var ctx=context(player,"impact",2);if(ConcordanceEffects.apply(ctx,new EffectSpec(EffectKind.DAMAGE,Intent.HARMFUL,amount,0,null,Stacking.STRONGEST,blind?"spell_power:arcane":"spell_power:frost"),victim).applied()){
            if(blind)ConcordanceEffects.apply(ctx,new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,0,100,"minecraft:blindness",Stacking.STRONGEST,null),victim);else victim.setTicksFrozen(Math.min(300,victim.getTicksFrozen()+100));
        }
    }
    @Override public boolean isPushable(){return false;}
    @Override protected void registerGoals(){}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);if(target!=null)out.store("target",UUIDUtil.CODEC,target);out.putInt("age",age);out.putInt("airTime",airTime);out.store("origin",BlockPos.CODEC,origin);out.putInt("face",face.ordinal());out.store("touched",UUIDUtil.CODEC.listOf(),new ArrayList<>(touched));}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);target=in.read("target",UUIDUtil.CODEC).orElse(null);age=in.getIntOr("age",301);airTime=in.getIntOr("airTime",0);origin=in.read("origin",BlockPos.CODEC).orElse(BlockPos.ZERO);face=Direction.values()[Math.clamp(in.getIntOr("face",1),0,5)];touched.addAll(in.read("touched",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(16).toList());}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<JimtaConstruct>("source",0,state->state.setAndContinue(kind.equals("spectral_hammer")?RawAnimation.begin().thenPlay("hammer_swing"):kind.equals("ice_block")?RawAnimation.begin().thenPlay("form"):age>=280?RawAnimation.begin().thenPlay("retreat"):RawAnimation.begin().thenPlay("rise").thenLoop("idle"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
