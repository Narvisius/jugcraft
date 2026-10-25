package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Native lobs, cursor-steered fireballs and persistent bomb residue, with bounded chaining. */
public final class JimtaShot extends Projectile {
    public static EntityType<JimtaShot> TYPE;
    private static final EntityDataAccessor<String> KIND=SynchedEntityData.defineId(JimtaShot.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<ItemStack> ITEM=SynchedEntityData.defineId(JimtaShot.class,EntityDataSerializers.ITEM_STACK);
    private static final Set<String> KINDS=Set.of("blaze_storm","flaming_barrage","starfall","lob_creeper","chain_creeper","snowball","magma_bomb","throw","arrow_volley","thunder_step","ember_field","frost_field","ice_shard","echo_arrow","ball_lightning");
    private int damage=1,generation,bounces;
    private final Map<UUID,Integer> lastHit=new HashMap<>();
    private UUID target;
    public void follow(LivingEntity victim){target=victim.getUUID();}
    private boolean resting;
    public JimtaShot(EntityType<? extends Projectile> type,Level level){super(type,level);setNoGravity(true);}
    public String kind(){return entityData.get(KIND);}
    public ItemStack visualItem(){return entityData.get(ITEM);}
    public boolean field(){return kind().endsWith("_field");}
    public static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_shot"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaShot>of(JimtaShot::new,MobCategory.MISC).sized(.35F,.35F).clientTrackingRange(14).updateInterval(1).build(key));}
    public static JimtaShot launch(ServerPlayer player,String kind,Vec3 origin,Vec3 velocity,int damage){
        var level=player.level();if(!KINDS.contains(kind)||!level.hasChunkAt(BlockPos.containing(origin))||!Authority.mayChange(level,player,BlockPos.containing(origin)))return null;
        if(level.getEntitiesOfClass(JimtaShot.class,player.getBoundingBox().inflate(128),e->e.getOwner()==player).size()>=80)return null;
        var shot=TYPE.create(level,EntitySpawnReason.TRIGGERED);if(shot==null)return null;shot.entityData.set(KIND,kind);shot.damage=Math.clamp(damage,0,60);shot.setOwner(player);shot.snapTo(origin.x,origin.y,origin.z,player.getYRot(),player.getXRot());shot.setDeltaMovement(velocity);
        shot.entityData.set(ITEM,kind.equals("throw")?player.getOffhandItem().copyWithCount(1):kind.contains("creeper")?new ItemStack(Items.CREEPER_HEAD):kind.equals("arrow_volley")?new ItemStack(Items.ARROW):ItemStack.EMPTY);
        return level.addFreshEntity(shot)?shot:null;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(KIND,"blaze_storm");builder.define(ITEM,ItemStack.EMPTY);}
    @Override public void tick(){
        super.tick();if(level().isClientSide())return;
        if(!(getOwner() instanceof ServerPlayer p)||!p.isAlive()||p.level()!=level()||!JimtaSpellbooks.enabled()||tickCount>(field()?200:kind().equals("thunder_step")?100:160)||distanceToSqr(p)>128*128){discard();return;}
        if(field()){if(tickCount%20==0)burst(p,position(),4,damage,kind().equals("ember_field")?"ignite":"chilled",false);if(tickCount%10==0)for(int i=0;i<12;i++)p.level().sendParticles(kind().equals("ember_field")?ParticleTypes.FLAME:ParticleTypes.SNOWFLAKE,getX()+Math.cos(i*Math.PI/6)*4,getY()+.2,getZ()+Math.sin(i*Math.PI/6)*4,1,0,.1,0,0);return;}
        if(resting)return;
        if(kind().equals("ball_lightning")){if(tickCount>=100){discard();return;}Vec3 end=position().add(getDeltaMovement());if(!level().hasChunkAt(BlockPos.containing(end))){discard();return;}var hit=level().clip(new ClipContext(position(),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));if(hit.getType()==HitResult.Type.BLOCK){var d=getDeltaMovement();setDeltaMovement(switch(hit.getDirection().getAxis()){case X->d.multiply(-1,1,1);case Y->d.multiply(1,-1,1);case Z->d.multiply(1,1,-1);});setPos(hit.getLocation().add(getDeltaMovement().normalize().scale(.1)));if(++bounces>=6)discard();}else setPos(end);for(var victim:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(.25),LivingEntity::isAlive).stream().limit(16).toList())if(tickCount-lastHit.getOrDefault(victim.getUUID(),-100)>=10&&hit(p,kind(),victim,damage,"",position()))lastHit.put(victim.getUUID(),tickCount);return;}
        Vec3 motion=getDeltaMovement();if(kind().equals("flaming_barrage")){var aim=p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(40)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();motion=motion.scale(.8).add(aim.subtract(position()).normalize().scale(.2));}
        else if(kind().equals("echo_arrow")&&target!=null&&p.level().getEntity(target) instanceof LivingEntity living){motion=motion.scale(.85).add(living.getBoundingBox().getCenter().subtract(position()).normalize().scale(.2));}
        else if(Set.of("ice_shard","snowball","magma_bomb","lob_creeper","chain_creeper","throw","arrow_volley").contains(kind()))motion=motion.add(0,-.035,0);
        Vec3 start=position(),end=start.add(motion);if(!level().hasChunkAt(BlockPos.containing(end))){discard();return;}
        var wall=level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));Vec3 stop=wall.getType()==HitResult.Type.BLOCK?wall.getLocation():end;LivingEntity hit=null;double nearest=start.distanceToSqr(stop);
        if(!kind().equals("thunder_step"))for(var e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().expandTowards(stop.subtract(start)).inflate(.3),e->e.isAlive()&&e!=p&&!JimtaSpellcasting.ally(p,e)).stream().limit(32).toList()){
            var point=e.getBoundingBox().inflate(.2).clip(start,stop);if(point.isPresent()&&start.distanceToSqr(point.get())<=nearest){hit=e;stop=point.get();nearest=start.distanceToSqr(stop);}}
        setDeltaMovement(motion);setPos(stop);
        if(wall.getType()==HitResult.Type.BLOCK||hit!=null){if(kind().equals("thunder_step")){resting=true;setDeltaMovement(Vec3.ZERO);return;}impact(p,stop,hit);discard();}
    }
    private void impact(ServerPlayer p,Vec3 at,LivingEntity direct){
        switch(kind()){
            case "snowball"->{burst(p,at,4,0,"chilled",false);launch(p,"frost_field",at,Vec3.ZERO,0);}
            case "magma_bomb"->{burst(p,at,4,damage,"ignite",false);launch(p,"ember_field",at,Vec3.ZERO,2);}
            case "lob_creeper","chain_creeper"->burst(p,at,5,damage,"",kind().equals("chain_creeper"));
            case "starfall"->burst(p,at,2,damage,"",false);
            default->{if(direct!=null){boolean applied=hit(p,kind(),direct,damage,kind().equals("blaze_storm")||kind().equals("flaming_barrage")?"ignite":"",at);if(applied&&kind().equals("ice_shard"))direct.setTicksFrozen(Math.min(300,direct.getTicksFrozen()+80));if(applied&&kind().equals("throw")&&!visualItem().isEmpty())net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffectsWithItemSource(p.level(),direct,p.damageSources().playerAttack(p),visualItem());}}
        }
        p.level().sendParticles((kind().equals("snowball")||kind().equals("ice_shard"))?ParticleTypes.SNOWFLAKE:kind().contains("creeper")?ParticleTypes.EXPLOSION:ParticleTypes.FLAME,at.x,at.y,at.z,12,.4,.3,.4,.05);
    }
    private void burst(ServerPlayer p,Vec3 at,double radius,int amount,String secondary,boolean chain){
        for(var victim:level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius),e->e.isAlive()&&!JimtaSpellcasting.ally(p,e)).stream().limit(16).toList()){
            double distance=victim.distanceToSqr(at);if(distance>=radius*radius)continue;int power=amount==0?0:Math.max(1,(int)(amount*(1-Math.pow(Math.sqrt(distance)/radius,kind().contains("creeper")?2:3))));
            if(hit(p,kind(),victim,power,secondary,at)&&chain&&!victim.isAlive()&&generation<2){for(int i=0;i<3;i++){var follow=launch(p,"chain_creeper",victim.getEyePosition(),new Vec3(Math.cos(i*Math.PI*2/3)*.3,.4,Math.sin(i*Math.PI*2/3)*.3),Math.max(1,(int)(damage*.85)));if(follow!=null)follow.generation=generation+1;}break;}
        }
    }
    public static boolean hit(ServerPlayer player,LivingEntity target,int damage,String secondary,Vec3 at){return hit(player,"projectile",target,damage,secondary,at);}
    public static boolean hit(ServerPlayer player,String spell,LivingEntity target,int damage,String secondary,Vec3 at){
        if(!target.isAlive()||JimtaSpellcasting.ally(player,target)||!TwoHanded.allowed(player,player.level(),target)||!ConcordanceEffects.mayHarm(player,target)||player.level().clip(new ClipContext(at,target.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()==HitResult.Type.BLOCK)return false;
        var ctx=new ConcordanceEffects.Context(player.level(),Cause.of(player.getUUID(),Cause.Origin.SPELL,"jugcraft:jimtafacts/"+spell,ConcordanceEffects.nextSerial()),player,new Ledger(new Ledger.Limits(2,8,0)),"impact",at);
        int previous=target.damageCooldownTime;boolean applied=false;
        try{target.damageCooldownTime=0;if(damage>0)applied=ConcordanceEffects.apply(ctx,EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,damage,0),target).applied();}
        finally{target.damageCooldownTime=Math.max(previous,target.damageCooldownTime);}
        if(!secondary.isEmpty())applied|=ConcordanceEffects.apply(ctx,new EffectSpec(secondary.equals("ignite")?EffectKind.NOUVEAU_CREATURE:EffectKind.STATUS,Intent.HARMFUL,secondary.equals("ignite")?1:0,secondary.equals("ignite")?40:200,secondary.equals("ignite")?"jugcraft:nouveau/ignite":"jugcraft:jimtafacts_chilled",Stacking.STRONGEST,null),target).applied();
        return applied;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(target!=null)out.store("target",UUIDUtil.CODEC,target);out.putString("kind",kind());out.putInt("damage",damage);out.putInt("age",tickCount);out.putInt("generation",generation);out.putInt("bounces",bounces);out.putBoolean("resting",resting);if(!visualItem().isEmpty())out.store("item",ItemStack.CODEC,visualItem());}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);target=in.read("target",UUIDUtil.CODEC).orElse(null);String kind=in.getStringOr("kind","blaze_storm");entityData.set(KIND,KINDS.contains(kind)?kind:"blaze_storm");entityData.set(ITEM,in.read("item",ItemStack.CODEC).orElse(ItemStack.EMPTY));damage=Math.clamp(in.getIntOr("damage",1),0,60);tickCount=in.getIntOr("age",201);generation=Math.clamp(in.getIntOr("generation",2),0,2);bounces=in.getIntOr("bounces",0);resting=in.getBooleanOr("resting",false);}
}
